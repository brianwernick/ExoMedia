/*
 * Copyright (C) 2015-2019 ExoMedia Contributors,
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.core.exoplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.FloatRange
import androidx.annotation.Size
import android.util.Log
import android.view.Surface
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.ExoMedia.RendererType
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.InternalErrorListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.renderer.RendererProvider
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.util.Repeater
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ExoMediaPlayer(private val context: Context) : Player.EventListener {
  companion object {
    private const val TAG = "ExoMediaPlayer"
    private const val BUFFER_REPEAT_DELAY = 1_000
    private const val WAKE_LOCK_TIMEOUT = 1_000
  }

  val exoPlayer: ExoPlayer
  private val trackSelector: DefaultTrackSelector
  private val adaptiveTrackSelectionFactory: AdaptiveTrackSelection.Factory
  private val mainHandler: Handler
  private val listeners = CopyOnWriteArrayList<ExoPlayerListener>()

  private val stopped = AtomicBoolean()
  private var prepared = false

  private val stateStore = StateStore()
  private val bufferRepeater = Repeater()

  private var surface: Surface? = null
  private var mediaSource: MediaSource? = null
  private val renderers: List<Renderer>
  private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

  private var captionListener: CaptionListener? = null
  private var metadataListener: MetadataListener? = null
  private var internalErrorListener: InternalErrorListener? = null
  private var bufferUpdateListener: OnBufferUpdateListener? = null

  private var wakeLock: PowerManager.WakeLock? = null

  var audioSessionId = C.AUDIO_SESSION_ID_UNSET
    private set

  @FloatRange(from = 0.0, to = 1.0)
  protected var requestedVolume = 1.0f

  /**
   * Returns the [AnalyticsCollector] used for collecting analytics events.
   */
  val analyticsCollector: AnalyticsCollector = AnalyticsCollector(Clock.DEFAULT)

  /**
   * Retrieves a list of available tracks
   *
   * @return A list of available tracks associated with each type
   */
  // Retrieves the available tracks
  // Maps the available tracks
  // collect track groups from all the track renderers of the same type
  // construct fake track group array for track groups from all the renderers of the same type
  val availableTracks: Map<RendererType, TrackGroupArray>?
    get() {
      if (playbackState == Player.STATE_IDLE) {
        return null
      }
      val trackMap = androidx.collection.ArrayMap<RendererType, TrackGroupArray>()
      val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return trackMap
      val types = arrayOf(RendererType.AUDIO, RendererType.VIDEO, RendererType.CLOSED_CAPTION, RendererType.METADATA)
      for (type in types) {
        val trackGroups = ArrayList<TrackGroup>()
        for (exoPlayerTrackIndex in getExoPlayerTracksInfo(type, 0, mappedTrackInfo).rendererTrackIndexes) {
          val trackGroupArray = mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex)
          for (i in 0 until trackGroupArray.length) {
            trackGroups.add(trackGroupArray.get(i))
          }
        }
        if (trackGroups.isNotEmpty()) {
          trackMap[type] = TrackGroupArray(*trackGroups.toTypedArray())
        }
      }

      return trackMap
    }

  var volume: Float
    @FloatRange(from = 0.0, to = 1.0)
    get() = requestedVolume
    set(@FloatRange(from = 0.0, to = 1.0) volume) {
      requestedVolume = volume
      sendMessage(C.TRACK_TYPE_AUDIO, Renderer.MSG_SET_VOLUME, requestedVolume)
    }

  val playbackState: Int
    get() = exoPlayer.playbackState

  val playbackSpeed: Float
    get() = exoPlayer.playbackParameters.speed

  val currentPosition: Long
    get() = getCurrentPosition(false)

  val duration: Long
    get() = exoPlayer.duration

  val bufferedPercentage: Int
    get() = exoPlayer.bufferedPercentage

  val windowInfo: WindowInfo?
    get() {
      val timeline = exoPlayer.currentTimeline
      if (timeline.isEmpty) {
        return null
      }

      val currentWindowIndex = exoPlayer.currentWindowIndex
      val currentWindow = timeline.getWindow(currentWindowIndex, Timeline.Window())

      return WindowInfo(
          exoPlayer.previousWindowIndex,
          currentWindowIndex,
          exoPlayer.nextWindowIndex,
          currentWindow
      )
    }

  var drmSessionManager: DrmSessionManager? = null

  var playWhenReady: Boolean
    get() = exoPlayer.playWhenReady
    set(playWhenReady) {
      exoPlayer.playWhenReady = playWhenReady
      stayAwake(playWhenReady)
    }

  init {
    bufferRepeater.repeaterDelay = BUFFER_REPEAT_DELAY
    bufferRepeater.repeatListener = {
      bufferUpdateListener?.onBufferingUpdate(bufferedPercentage)
    }

    mainHandler = Handler(Looper.getMainLooper())

    val componentListener = ComponentListener(analyticsCollector)
    val rendererProvider = RendererProvider(context, mainHandler, componentListener, componentListener, componentListener, componentListener)

    renderers = rendererProvider.generate()

    adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
    trackSelector = DefaultTrackSelector(context, adaptiveTrackSelectionFactory)

    val loadControl = ExoMedia.Data.loadControl ?: DefaultLoadControl()
    exoPlayer = ExoPlayer.Builder(renderers.toTypedArray(), trackSelector, DefaultMediaSourceFactory(context), loadControl, bandwidthMeter)
        .setAnalyticsCollector(analyticsCollector)
        .build()
    exoPlayer.addListener(this)
    exoPlayer.addListener(analyticsCollector)
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
    reportPlayerState()
  }

  override fun onPlayerError(error: ExoPlaybackException) {
    listeners.forEach {
      it.onError(this, error)
    }
  }

  fun setUri(uri: Uri?) {
    val mediaSource = uri?.let { ExoMedia.Data.mediaSourceProvider.generate(context, mainHandler, it, bandwidthMeter, drmSessionManager) }
    setMediaSource(mediaSource)
  }

  fun setMediaSource(source: MediaSource?) {
    mediaSource?.let {
      it.removeEventListener(analyticsCollector)
      analyticsCollector.resetForNewPlaylist()
    }

    source?.addEventListener(mainHandler, analyticsCollector)
    this.mediaSource = source

    prepared = false
    prepare()
  }

  fun addListener(listener: ExoPlayerListener?) {
    if (listener != null) {
      listeners.add(listener)
    }
  }

  fun removeListener(listener: ExoPlayerListener?) {
    if (listener != null) {
      listeners.remove(listener)
    }
  }

  fun setBufferUpdateListener(listener: OnBufferUpdateListener?) {
    this.bufferUpdateListener = listener
    setBufferRepeaterStarted(listener != null)
  }

  fun setInternalErrorListener(listener: InternalErrorListener?) {
    internalErrorListener = listener
  }

  fun setCaptionListener(listener: CaptionListener?) {
    captionListener = listener
  }

  fun setMetadataListener(listener: MetadataListener?) {
    metadataListener = listener
  }

  fun setSurface(surface: Surface?) {
    this.surface = surface
    sendMessage(C.TRACK_TYPE_VIDEO, Renderer.MSG_SET_SURFACE, surface, false)
  }

  fun getSurface(): Surface? {
    return surface
  }

  fun getBandwidthMeter(): BandwidthMeter {
    return bandwidthMeter
  }

  /**
   * Adds an [AnalyticsListener] to receive analytics events.
   *
   * @param listener The repeatListener to be added.
   */
  fun addAnalyticsListener(listener: AnalyticsListener) {
    analyticsCollector.addListener(listener)
  }

  /**
   * Removes an [AnalyticsListener].
   *
   * @param listener The repeatListener to be removed.
   */
  fun removeAnalyticsListener(listener: AnalyticsListener) {
    analyticsCollector.removeListener(listener)
  }

  fun clearSurface() {
    surface?.release()

    surface = null
    sendMessage(C.TRACK_TYPE_VIDEO, Renderer.MSG_SET_SURFACE, null, false)
  }

  @JvmOverloads
  fun getSelectedTrackIndex(type: RendererType, groupIndex: Int = 0): Int {
    // Retrieves the available tracks
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo)
    val trackGroupArray = if (tracksInfo.rendererTrackIndex == C.INDEX_UNSET)
      null
    else
      mappedTrackInfo!!.getTrackGroups(tracksInfo.rendererTrackIndex)
    if (trackGroupArray == null || trackGroupArray.length == 0) {
      return -1
    }

    // Verifies the track selection has been overridden
    val selectionOverride = trackSelector.parameters.getSelectionOverride(tracksInfo.rendererTrackIndex, trackGroupArray)
    return if (selectionOverride == null || selectionOverride.groupIndex != tracksInfo.rendererTrackGroupIndex || selectionOverride.length <= 0) {
      -1
    } else selectionOverride.tracks[0]

    // In the current implementation only one track can be selected at a time so get the first one.
  }

  fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    // Retrieves the available tracks
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo)
    val trackGroupArray = if (tracksInfo.rendererTrackIndex == C.INDEX_UNSET || mappedTrackInfo == null)
      null
    else
      mappedTrackInfo.getTrackGroups(tracksInfo.rendererTrackIndex)
    if (trackGroupArray == null || trackGroupArray.length == 0 || trackGroupArray.length <= tracksInfo.rendererTrackGroupIndex) {
      return
    }

    // Finds the requested group
    val group = trackGroupArray.get(tracksInfo.rendererTrackGroupIndex)
    if (group == null || group.length <= trackIndex) {
      return
    }

    val parametersBuilder = trackSelector.buildUponParameters()
    for (rendererTrackIndex in tracksInfo.rendererTrackIndexes) {
      parametersBuilder.clearSelectionOverrides(rendererTrackIndex)
      if (tracksInfo.rendererTrackIndex == rendererTrackIndex) {
        // Specifies the correct track to use
        parametersBuilder.setSelectionOverride(rendererTrackIndex, trackGroupArray,
            DefaultTrackSelector.SelectionOverride(tracksInfo.rendererTrackGroupIndex, trackIndex))
        // make sure renderer is enabled
        parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
      } else {
        // disable other renderers of the same type to avoid playback errors
        parametersBuilder.setRendererDisabled(rendererTrackIndex, true)
      }
    }
    trackSelector.setParameters(parametersBuilder)
  }

  /**
   * Clear all selected tracks for the specified renderer and re-enable all renderers so the player can select the default track.
   *
   * @param type The renderer type
   */
  fun clearSelectedTracks(type: RendererType) {
    // Retrieves the available tracks
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    val parametersBuilder = trackSelector.buildUponParameters()

    for (rendererTrackIndex in tracksInfo.rendererTrackIndexes) {
      // Reset all renderers re-enabling so the player can select the streams default track.
      parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
          .clearSelectionOverrides(rendererTrackIndex)
    }
    trackSelector.setParameters(parametersBuilder)
  }

  fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    if (tracksInfo.rendererTrackIndexes.isNotEmpty()) {
      var enabledSomething = false
      val parametersBuilder = trackSelector.buildUponParameters()
      for (rendererTrackIndex in tracksInfo.rendererTrackIndexes) {
        if (enabled) {
          val selectionOverride = trackSelector.parameters.getSelectionOverride(rendererTrackIndex, mappedTrackInfo!!.getTrackGroups(rendererTrackIndex))
          // check whether the renderer has been selected before
          // other renderers should be kept disabled to avoid playback errors
          if (selectionOverride != null) {
            parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
            enabledSomething = true
          }
        } else {
          parametersBuilder.setRendererDisabled(rendererTrackIndex, true)
        }
      }
      if (enabled && !enabledSomething) {
        // if nothing has been enabled enable the first sequential renderer
        parametersBuilder.setRendererDisabled(tracksInfo.rendererTrackIndexes[0], false)
      }
      trackSelector.setParameters(parametersBuilder)
    }
  }

  /**
   * Return true if at least one renderer for the given type is enabled
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: RendererType): Boolean {
    val mappedTrackInfo = trackSelector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    val parameters = trackSelector.parameters
    for (rendererTrackIndex in tracksInfo.rendererTrackIndexes) {
      if (!parameters.getRendererDisabled(rendererTrackIndex)) {
        return true
      }
    }
    return false
  }

  fun setAudioStreamType(streamType: Int) {
    @C.AudioUsage
    val usage = Util.getAudioUsageForStreamType(streamType)

    @C.AudioContentType
    val contentType = Util.getAudioContentTypeForStreamType(streamType)

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(usage)
        .setContentType(contentType)
        .build()

    sendMessage(C.TRACK_TYPE_AUDIO, Renderer.MSG_SET_AUDIO_ATTRIBUTES, audioAttributes)
  }

  fun forcePrepare() {
    prepared = false
  }

  fun prepare() {
    val source = mediaSource
    if (prepared || source == null) {
      return
    }

    if (renderers.isNotEmpty()) {
      exoPlayer.stop()
    }

    stateStore.reset()
    exoPlayer.setMediaSource(source)
    exoPlayer.prepare()
    prepared = true

    stopped.set(false)
  }

  fun stop() {
    if (!stopped.getAndSet(true)) {
      exoPlayer.playWhenReady = false
      exoPlayer.stop()
    }
  }

  /**
   * TODO: Expose this
   * Seeks to the specified position in the media currently loaded specified by `positionMs`.
   * If `limitToCurrentWindow` is true then a seek won't be allowed to span across windows.
   * This should only be different if the media in playback has multiple windows (e.g. in the case of using a
   * `ConcatenatingMediaSource` with more than 1 source)
   *
   * @param positionMs           The position to seek to in the media
   * @param limitToCurrentWindow `true` to only seek in the current window
   */
  @JvmOverloads
  fun seekTo(positionMs: Long, limitToCurrentWindow: Boolean = false) {
    analyticsCollector.notifySeekStarted()
    if (limitToCurrentWindow) {
      exoPlayer.seekTo(positionMs)
      stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady, StateStore.STATE_SEEKING)
      return
    }

    // We seek to the position in the timeline (may be across windows)
    val timeline = exoPlayer.currentTimeline
    val windowCount = timeline.windowCount

    var cumulativePositionMs: Long = 0
    val window = Timeline.Window()

    for (index in 0 until windowCount) {
      timeline.getWindow(index, window)

      val windowDurationMs = window.durationMs
      if (cumulativePositionMs < positionMs && positionMs <= cumulativePositionMs + windowDurationMs) {
        exoPlayer.seekTo(index, positionMs - cumulativePositionMs)
        stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady, StateStore.STATE_SEEKING)
        return
      }

      cumulativePositionMs += windowDurationMs
    }

    Log.e(TAG, "Unable to seek across windows, falling back to in-window seeking")
    exoPlayer.seekTo(positionMs)
    stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady, StateStore.STATE_SEEKING)
  }

  /**
   * Seeks to the beginning of the media, and plays it. This method will not succeed if playback state is not `ExoPlayer.STATE_IDLE` or `ExoPlayer.STATE_ENDED`.
   *
   * @return `true` if the media was successfully restarted, otherwise `false`
   */
  fun restart(): Boolean {
    val playbackState = playbackState
    if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
      return false
    }

    seekTo(0)
    playWhenReady = true

    forcePrepare()
    prepare()

    return true
  }

  fun release() {
    setBufferRepeaterStarted(false)
    listeners.clear()

    mediaSource?.removeEventListener(analyticsCollector)

    surface = null
    exoPlayer.release()
    stayAwake(false)
  }

  fun setPlaybackSpeed(speed: Float): Boolean {
    exoPlayer.setPlaybackParameters(PlaybackParameters(speed, 1.0f))

    return true
  }

  /**
   * TODO: Expose this
   * Returns the position in the media. If `limitToCurrentWindow` is `true` then the position
   * in the current window will be returned, otherwise the total position across all windows will be returned.
   * These should only be different if the media in playback has multiple windows (e.g. in the case of using a
   * `ConcatenatingMediaSource` with more than 1 source)
   *
   * @param limitToCurrentWindow If `true` the position within the current window will be returned
   * @return The current position in the media
   */
  fun getCurrentPosition(limitToCurrentWindow: Boolean): Long {
    val positionInCurrentWindow = exoPlayer.currentPosition
    if (limitToCurrentWindow) {
      return positionInCurrentWindow
    }

    // TODO cache the total time at the start of each window (e.g. Map<WindowIndex, cumulativeStartTimeMs>)
    // Adds the preceding window durations
    val timeline = exoPlayer.currentTimeline
    val maxWindowIndex = min(timeline.windowCount - 1, exoPlayer.currentWindowIndex)

    var cumulativePositionMs: Long = 0
    val window = Timeline.Window()

    for (index in 0 until maxWindowIndex) {
      timeline.getWindow(index, window)
      cumulativePositionMs += window.durationMs
    }

    return cumulativePositionMs + positionInCurrentWindow
  }

  /**
   * This function has the MediaPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring.
   * The parameter is a combination of [android.os.PowerManager] wake flags.
   * Use of this method requires [android.Manifest.permission.WAKE_LOCK]
   * permission.
   * By default, no attempt is made to keep the device awake during playback.
   *
   * @param context the Context to use
   * @param mode    the power/wake mode to set
   * @see android.os.PowerManager
   */
  fun setWakeMode(context: Context, mode: Int) {
    var wasHeld = false
    wakeLock?.let { lock ->
      if (lock.isHeld) {
        wasHeld = true
        lock.release()
      }

      wakeLock = null
    }

    //Acquires the wakelock if we have permissions to
    if (context.packageManager.checkPermission(Manifest.permission.WAKE_LOCK, context.packageName) == PackageManager.PERMISSION_GRANTED) {
      val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = pm.newWakeLock(mode or PowerManager.ON_AFTER_RELEASE, ExoMediaPlayer::class.java.name).apply {
        setReferenceCounted(false)
      }
    } else {
      Log.w(TAG, "Unable to acquire WAKE_LOCK due to missing manifest permission")
    }

    stayAwake(wasHeld)
  }

  fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    exoPlayer.repeatMode = repeatMode
  }

  protected fun getExoPlayerTrackType(type: RendererType): Int {
    return when (type) {
      RendererType.AUDIO -> C.TRACK_TYPE_AUDIO
      RendererType.VIDEO -> C.TRACK_TYPE_VIDEO
      RendererType.CLOSED_CAPTION -> C.TRACK_TYPE_TEXT
      RendererType.METADATA -> C.TRACK_TYPE_METADATA
    }
  }

  protected fun getExoMediaRendererType(exoPlayerTrackType: Int): RendererType? {
    return when (exoPlayerTrackType) {
      C.TRACK_TYPE_AUDIO -> RendererType.AUDIO
      C.TRACK_TYPE_VIDEO -> RendererType.VIDEO
      C.TRACK_TYPE_TEXT -> RendererType.CLOSED_CAPTION
      C.TRACK_TYPE_METADATA -> RendererType.METADATA
      else -> null
    }
  }

  protected fun getExoPlayerTracksInfo(type: RendererType, groupIndex: Int, mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?): ExoPlayerRendererTracksInfo {
    // holder for the all exo player renderer track indexes of the specified renderer type
    val exoPlayerRendererTrackIndexes = ArrayList<Int>()
    // the exoplayer renderer track index related to the specified group index
    var exoPlayerRendererTrackIndex = C.INDEX_UNSET
    // the corrected exoplayer group index
    var exoPlayerRendererTrackGroupIndex = C.INDEX_UNSET
    var skippedRenderersGroupsCount = 0
    if (mappedTrackInfo != null) {
      for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
        val exoPlayerRendererType = mappedTrackInfo.getRendererType(rendererIndex)
        if (type == getExoMediaRendererType(exoPlayerRendererType)) {
          exoPlayerRendererTrackIndexes.add(rendererIndex)
          val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
          if (skippedRenderersGroupsCount + trackGroups.length > groupIndex) {
            if (exoPlayerRendererTrackIndex == C.INDEX_UNSET) {
              // if the groupIndex belongs to the current exo player renderer
              exoPlayerRendererTrackIndex = rendererIndex
              exoPlayerRendererTrackGroupIndex = groupIndex - skippedRenderersGroupsCount
            }
          } else {
            skippedRenderersGroupsCount += trackGroups.length
          }
        }
      }
    }
    return ExoPlayerRendererTracksInfo(exoPlayerRendererTrackIndexes, exoPlayerRendererTrackIndex, exoPlayerRendererTrackGroupIndex)
  }

  inner class ExoPlayerRendererTracksInfo(
      rendererTrackIndexes: List<Int>,
      /**
       * The renderer track index related to the requested `groupIndex`
       */
      val rendererTrackIndex: Int,
      /**
       * The corrected exoplayer group index which may be used to obtain proper track group from the renderer
       */
      val rendererTrackGroupIndex: Int) {
    /**
     * The exo player renderer track indexes
     */
    val rendererTrackIndexes: List<Int>

    init {
      this.rendererTrackIndexes = Collections.unmodifiableList(rendererTrackIndexes)
    }
  }

  @JvmOverloads
  protected fun sendMessage(renderType: Int, messageType: Int, message: Any?, blocking: Boolean = false) {
    if (renderers.isEmpty()) {
      return
    }

    val messages = ArrayList<PlayerMessage>()
    for (renderer in renderers) {
      if (renderer.trackType == renderType) {
        messages.add(exoPlayer.createMessage(renderer).setType(messageType).setPayload(message))
      }
    }

    if (blocking) {
      blockingSendMessages(messages)
    } else {
      for (playerMessage in messages) {
        playerMessage.send()
      }
    }
  }

  /**
   * This was pulled from the *Deprecated* ExoPlayerImpl#blockingSendMessages method
   *
   * @param messages The messages
   */
  protected fun blockingSendMessages(messages: List<PlayerMessage>) {
    var wasInterrupted = false
    for (message in messages) {
      var blockMessage = true
      while (blockMessage) {
        try {
          message.blockUntilDelivered()
          blockMessage = false
        } catch (e: InterruptedException) {
          wasInterrupted = true
        }

      }
    }

    if (wasInterrupted) {
      // Restore the interrupted status.
      Thread.currentThread().interrupt()
    }
  }

  /**
   * Used with playback state changes to correctly acquire and
   * release the wakelock if the user has enabled it with [.setWakeMode].
   * If the [.wakeLock] is null then no action will be performed.
   *
   * @param awake True if the wakelock should be acquired
   */
  protected fun stayAwake(awake: Boolean) {
    wakeLock?.let { lock ->
      if (awake && !lock.isHeld) {
        lock.acquire(WAKE_LOCK_TIMEOUT.toLong())
      } else if (!awake && lock.isHeld) {
        lock.release()
      }
    }
  }

  private fun reportPlayerState() {
    val playWhenReady = exoPlayer.playWhenReady
    val playbackState = playbackState

    val newState = stateStore.getState(playWhenReady, playbackState)
    if (newState != stateStore.mostRecentState) {
      stateStore.setMostRecentState(playWhenReady, playbackState)

      //Makes sure the buffering notifications are sent
      if (newState == Player.STATE_READY) {
        setBufferRepeaterStarted(true)
      } else if (newState == Player.STATE_IDLE || newState == Player.STATE_ENDED) {
        setBufferRepeaterStarted(false)
      }

      //Because the playWhenReady isn't a state in itself, rather a flag to a state we will ignore informing of
      // see events when that is the only change.  Additionally, on some devices we get states ordered as
      // [seeking, ready, buffering, ready] while on others we get [seeking, buffering, ready]
      var informSeekCompletion = stateStore.matchesHistory(intArrayOf(StateStore.STATE_SEEKING, Player.STATE_BUFFERING, Player.STATE_READY), true)
      informSeekCompletion = informSeekCompletion or stateStore.matchesHistory(intArrayOf(Player.STATE_BUFFERING, StateStore.STATE_SEEKING, Player.STATE_READY), true)
      informSeekCompletion = informSeekCompletion or stateStore.matchesHistory(intArrayOf(StateStore.STATE_SEEKING, Player.STATE_READY, Player.STATE_BUFFERING, Player.STATE_READY), true)

      for (listener in listeners) {
        listener.onStateChanged(playWhenReady, playbackState)

        if (informSeekCompletion) {
          listener.onSeekComplete()
        }
      }
    }
  }

  private fun setBufferRepeaterStarted(start: Boolean) {
    if (start && bufferUpdateListener != null) {
      bufferRepeater.start()
    } else {
      bufferRepeater.stop()
    }
  }

  private class StateStore {
    companion object {
      const val FLAG_PLAY_WHEN_READY = -0x10000000
      const val STATE_SEEKING = 100
    }

    //We keep the last few states because that is all we need currently
    private val prevStates = intArrayOf(Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE)

    val mostRecentState: Int
      get() = prevStates[3]

    val isLastReportedPlayWhenReady: Boolean
      get() = prevStates[3] and FLAG_PLAY_WHEN_READY != 0

    fun reset() {
      for (i in prevStates.indices) {
        prevStates[i] = Player.STATE_IDLE
      }
    }

    fun setMostRecentState(playWhenReady: Boolean, state: Int) {
      val newState = getState(playWhenReady, state)
      if (prevStates[3] == newState) {
        return
      }

      prevStates[0] = prevStates[1]
      prevStates[1] = prevStates[2]
      prevStates[2] = prevStates[3]
      prevStates[3] = state
    }

    fun getState(playWhenReady: Boolean, state: Int): Int {
      return state or if (playWhenReady) FLAG_PLAY_WHEN_READY else 0
    }

    fun matchesHistory(@Size(min = 1, max = 4) states: IntArray, ignorePlayWhenReady: Boolean): Boolean {
      var flag = true
      val andFlag = if (ignorePlayWhenReady) FLAG_PLAY_WHEN_READY.inv() else 0x0.inv()
      val startIndex = prevStates.size - states.size

      for (i in startIndex until prevStates.size) {
        flag = flag and (prevStates[i] and andFlag == states[i - startIndex] and andFlag)
      }

      return flag
    }
  }

  private inner class ComponentListener(delegate: AnalyticsCollector): VideoRendererEventListener, AudioRendererEventListener, TextOutput, MetadataOutput, DrmSessionEventListener by delegate {

    override fun onAudioDisabled(counters: DecoderCounters) {
      audioSessionId = C.AUDIO_SESSION_ID_UNSET
      analyticsCollector.onAudioDisabled(counters)
    }

    override fun onAudioSessionId(sessionId: Int) {
      audioSessionId = sessionId
      analyticsCollector.onAudioSessionId(sessionId)
    }

    override fun onAudioUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
      internalErrorListener?.onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
      analyticsCollector.onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
    }

    override fun onDrmSessionManagerError(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, error: java.lang.Exception) {
      internalErrorListener?.onDrmSessionManagerError(error)
      analyticsCollector.onDrmSessionManagerError(windowIndex, mediaPeriodId, error)
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      for (listener in listeners) {
        listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
      }
      analyticsCollector.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
    }

    override fun onMetadata(metadata: Metadata) {
      metadataListener?.onMetadata(metadata)
      analyticsCollector.onMetadata(metadata)
    }

    override fun onCues(cues: List<Cue>) {
      captionListener?.onCues(cues)
    }
  }
}