package com.devbrackets.android.exomedia.nmp

import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.FloatRange
import androidx.media3.common.*
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.manager.StateStore
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.devbrackets.android.exomedia.util.Repeater
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ExoMediaPlayerImpl(
  private val config: PlayerConfig
) : Player.Listener, ExoMediaPlayer {
  companion object {
    private const val TAG = "ExoMediaPlayer"
    private const val BUFFER_REPEAT_DELAY = 1_000L

    // The amount of time the current position can be off the duration to call the onCompletion listener
    private const val COMPLETED_DURATION_LEEWAY: Long = 1_000
  }

  private val listeners = CopyOnWriteArrayList<ExoPlayerListener>()
  private val rendererListener = DelegatedRenderListener()

  private val exoPlayer: ExoPlayer = ExoPlayer.Builder(
    config.context,
    config.rendererFactory,
    config.mediaSourceFactory,
    config.trackManager.selector,
    config.loadControl,
    config.bandwidthMeter,
    config.analyticsCollector
  ).build().also {
    it.addListener(this)
    it.addListener(rendererListener)
    it.addListener(config.analyticsCollector)
  }

  private val stopped = AtomicBoolean()
  private var prepared = false

  private val stateStore = StateStore()
  private val bufferRepeater by lazy {
    Repeater(
      delayMillis = BUFFER_REPEAT_DELAY,
      callback = {
        bufferUpdateListener?.onBufferingUpdate(bufferedPercent)
      }
    ).apply {
      start()
    }
  }

  override var surface: Surface? = null
    set(value) {
      field = value
      exoPlayer.setVideoSurface(value)
    }

  private var mediaSource: MediaSource? = null

  private var bufferUpdateListener: OnBufferUpdateListener? = null

  @FloatRange(from = 0.0, to = 1.0)
  private var requestedVolume = 1.0f

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = config.trackManager.getAvailableTracks()

  override var volume: Float
    get() = requestedVolume
    set(volume) {
      requestedVolume = volume.coerceIn(0.0F, 1.0F)
      exoPlayer.volume = requestedVolume
    }

  override var playbackState = PlaybackState.IDLE
    private set

  override var playbackSpeed: Float
    get() = exoPlayer.playbackParameters.speed
    set(value) {
      exoPlayer.playbackParameters = PlaybackParameters(value, exoPlayer.playbackParameters.pitch)
    }

  override var playbackPitch: Float
    get() = exoPlayer.playbackParameters.pitch
    set(value) {
      exoPlayer.playbackParameters = PlaybackParameters(exoPlayer.playbackParameters.speed, value)
    }

  override val currentPosition: Long
    get() = getCurrentPosition(false)

  override val duration: Long
    get() = exoPlayer.duration

  override val bufferedPercent: Int
    get() = exoPlayer.bufferedPercentage

  override var drmSessionManagerProvider: DrmSessionManagerProvider? = null

  override val windowInfo: WindowInfo?
    get() {
      val timeline = exoPlayer.currentTimeline
      if (timeline.isEmpty) {
        return null
      }

      val currentWindowIndex = exoPlayer.currentMediaItemIndex
      val currentWindow = timeline.getWindow(currentWindowIndex, Timeline.Window())

      return WindowInfo(
        exoPlayer.previousMediaItemIndex,
        currentWindowIndex,
        exoPlayer.nextMediaItemIndex,
        currentWindow
      )
    }

  override val audioSessionId: Int
    get() = exoPlayer.audioSessionId

  override val playing: Boolean
    get() = exoPlayer.isPlaying

  override var playWhenReady: Boolean
    get() = exoPlayer.playWhenReady
    set(playWhenReady) {
      exoPlayer.playWhenReady = playWhenReady
      config.wakeManager.stayAwake(playWhenReady)
    }

  override fun onPlaybackStateChanged(playbackState: Int) {
    reportExoPlayerState()
  }

  override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
    reportExoPlayerState()
  }

  override fun onPlayerError(error: PlaybackException) {
    reportState(PlaybackState.ERROR)

    listeners.forEach {
      it.onError(this, error)
    }
  }

  override fun setMediaUri(uri: Uri?) {
    val mediaSource = uri?.let {
      val attributes = MediaSourceBuilder.MediaSourceAttributes(
        context = config.context,
        uri = it,
        handler = config.handler,
        userAgent = config.userAgentProvider.userAgent,
        transferListener = config.bandwidthMeter.transferListener,
        drmSessionManagerProvider = drmSessionManagerProvider ?: DefaultDrmSessionManagerProvider(),
        dataSourceFactoryProvider = config.dataSourceFactoryProvider
      )

      config.mediaSourceProvider.generate(attributes)
    }

    setMediaSource(mediaSource)
  }

  override fun setMediaSource(source: MediaSource?) {
    mediaSource?.removeEventListener(config.analyticsCollector)
    source?.addEventListener(config.handler, config.analyticsCollector)
    this.mediaSource = source

    prepared = false
    prepare()
  }

  override fun addListener(listener: ExoPlayerListener) {
    listeners.add(listener)
  }

  override fun removeListener(listener: ExoPlayerListener) {
    listeners.remove(listener)
  }

  override fun setBufferUpdateListener(listener: OnBufferUpdateListener?) {
    this.bufferUpdateListener = listener
  }

  override fun setMetadataListener(listener: MetadataListener?) {
    rendererListener.setMetadataListener(listener)
  }

  override fun setCaptionListener(listener: CaptionListener?) {
    rendererListener.setCaptionListener(listener)
  }

  override fun setVideoSizeListener(listener: VideoSizeListener?) {
    rendererListener.setVideoSizeListener(listener)
  }

  override fun addAnalyticsListener(listener: AnalyticsListener) {
    config.analyticsCollector.addListener(listener)
  }

  override fun removeAnalyticsListener(listener: AnalyticsListener) {
    config.analyticsCollector.removeListener(listener)
  }

  override fun clearSurface() {
    surface?.release()
    surface = null

    exoPlayer.clearVideoSurface()
  }

  @Deprecated("Use setAudioAttributes instead")
  override fun setAudioStreamType(streamType: Int) {
    val usage = Util.getAudioUsageForStreamType(streamType)
    val contentType = Util.getAudioContentTypeForStreamType(streamType)

    val audioAttributes = AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(contentType)
      .build()

    setAudioAttributes(audioAttributes)
  }

  override fun setAudioAttributes(attributes: AudioAttributes) {
    exoPlayer.setAudioAttributes(attributes, false)
  }

  override fun clearSelectedTracks(type: RendererType) {
    config.trackManager.clearSelectedTracks(type)
  }

  override fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    config.trackManager.setRendererEnabled(type, enabled)
  }

  override fun isRendererEnabled(type: RendererType): Boolean {
    return config.trackManager.isRendererEnabled(type)
  }

  override fun forcePrepare() {
    prepared = false
  }

  override fun prepare() {
    val source = mediaSource
    if (prepared || source == null) {
      return
    }

    reportState(PlaybackState.PREPARING)

    exoPlayer.stop()
    stateStore.reset()
    exoPlayer.setMediaSource(source)
    exoPlayer.prepare()
    prepared = true

    stopped.set(false)
  }

  override fun start() {
    playWhenReady = true
  }

  override fun pause() {
    playWhenReady = false
  }

  override fun restart(): Boolean {
    val playbackState = exoPlayer.playbackState
    if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
      return false
    }

    seekTo(0)
    playWhenReady = true

    forcePrepare()
    prepare()

    return true
  }

  override fun stop() {
    if (!stopped.getAndSet(true)) {
      exoPlayer.playWhenReady = false
      exoPlayer.stop()
      reportState(PlaybackState.STOPPED)
    }
  }

  override fun release() {
    bufferRepeater.stop()
    listeners.clear()

    mediaSource?.removeEventListener(config.analyticsCollector)

    surface = null
    playWhenReady = false
    exoPlayer.release()
    config.wakeManager.stayAwake(false)
    reportState(PlaybackState.RELEASED)
  }

  override fun seekTo(positionMs: Long) {
    seekTo(positionMs, false)
  }

  override fun seekTo(positionMs: Long, limitToCurrentWindow: Boolean) {
    config.analyticsCollector.notifySeekStarted()
    reportState(PlaybackState.SEEKING)

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

  override fun getCurrentPosition(limitToCurrentWindow: Boolean): Long {
    val positionInCurrentWindow = exoPlayer.currentPosition
    if (limitToCurrentWindow) {
      return positionInCurrentWindow
    }

    // TODO cache the total time at the start of each window (e.g. Map<WindowIndex, cumulativeStartTimeMs>)
    // Adds the preceding window durations
    val timeline = exoPlayer.currentTimeline
    val maxWindowIndex = min(timeline.windowCount - 1, exoPlayer.currentMediaItemIndex)

    var cumulativePositionMs: Long = 0
    val window = Timeline.Window()

    for (index in 0 until maxWindowIndex) {
      timeline.getWindow(index, window)
      cumulativePositionMs += window.durationMs
    }

    return cumulativePositionMs + positionInCurrentWindow
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    config.wakeManager.setWakeLevel(levelAndFlags)
  }

  override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    exoPlayer.repeatMode = repeatMode
  }

  override fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    return config.trackManager.setSelectedTrack(type, groupIndex, trackIndex)
  }

  override fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int {
    return config.trackManager.getSelectedTrackIndex(type, groupIndex)
  }

  private fun reportExoPlayerState() {
    val playWhenReady = exoPlayer.playWhenReady
    val playbackState = exoPlayer.playbackState

    // Don't report duplicate states
    val newState = stateStore.getState(playWhenReady, playbackState)
    if (newState == stateStore.mostRecentState) {
      return
    }

    stateStore.setMostRecentState(playWhenReady, playbackState)
    val informSeekCompletion = stateStore.seekCompleted()

    reportState(determinePlaybackState(playbackState, informSeekCompletion))

    if (informSeekCompletion) {
      listeners.forEach { listener ->
        listener.onSeekComplete()
      }
    }
  }

  private fun reportState(state: PlaybackState) {
    playbackState = state

    listeners.forEach { listener ->
      listener.onPlaybackStateChange(state)
    }
  }

  private fun determinePlaybackState(@Player.State state: Int, seekCompleted: Boolean): PlaybackState {
    // Seek Completion
    if (seekCompleted && state == Player.STATE_READY) {
      return when (playWhenReady) {
        true -> PlaybackState.PLAYING
        false -> PlaybackState.READY
      }
    }

    // Check for Playback
    if (playWhenReady && state == Player.STATE_READY) {
      return PlaybackState.PLAYING
    }

    // Check for a Pause
    if (stateStore.paused()) {
        return PlaybackState.PAUSED
    }

    // Handle Ended
    if (state == Player.STATE_ENDED) {
      return determineEndedState()
    }

    // Standard mapping
    return when (state) {
      Player.STATE_IDLE -> PlaybackState.IDLE
      Player.STATE_BUFFERING -> PlaybackState.BUFFERING
      Player.STATE_READY -> PlaybackState.READY
      else -> PlaybackState.IDLE
    }
  }

  /**
   * The ExoPlayer will report [Player.STATE_ENDED] in a few circumstances that map to one of the below. This
   * means that we need to determine which state is actually being represented:
   *  - COMPLETED
   *  - STOPPED
   *  - RELEASED
   *  - ERROR
   */
  private fun determineEndedState(): PlaybackState {
    when (playbackState) {
      PlaybackState.COMPLETED -> return PlaybackState.COMPLETED
      PlaybackState.RELEASED -> return PlaybackState.RELEASED
      PlaybackState.STOPPED -> return PlaybackState.STOPPED
      PlaybackState.ERROR -> return PlaybackState.ERROR
      else -> {}
    }

    val completed = currentPosition > 0 && duration > 0 && currentPosition + COMPLETED_DURATION_LEEWAY >= duration
    return if (completed) {
      PlaybackState.COMPLETED
    } else {
      PlaybackState.STOPPED
    }
  }
}