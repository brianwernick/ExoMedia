/*
 * Copyright (C) 2015-2021 ExoMedia Contributors,
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

package com.devbrackets.android.exomedia.nmp

import android.net.Uri
import androidx.annotation.FloatRange
import android.util.Log
import android.view.Surface
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.nmp.manager.StateStore
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.util.Util
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer
import kotlin.math.min

class ExoMediaPlayerImpl(
    private val config: PlayerConfig
) : Player.EventListener, ExoMediaPlayer {
  companion object {
    private const val TAG = "ExoMediaPlayer"
    private const val BUFFER_REPEAT_DELAY = 1_000L
  }

  private val exoPlayer: ExoPlayer by lazy {
    ExoPlayer.Builder(
        renderers.toTypedArray(),
        config.trackManager.selector,
        config.mediaSourceFactory,
        config.loadControl,
        config.bandwidthMeter
    ).setAnalyticsCollector(config.analyticsCollector)
        .build().also {
          it.addListener(this)
          it.addListener(config.analyticsCollector)
        }
  }

  private val listeners = CopyOnWriteArrayList<ExoPlayerListener>()

  // TODO: shouldn't stopped/prepared be in the state store?
  private val stopped = AtomicBoolean()
  private var prepared = false

  private val stateStore = StateStore()
  private val bufferTimer by lazy {
    fixedRateTimer("bufferRepeater", true, 0L, BUFFER_REPEAT_DELAY) {
      bufferUpdateListener?.onBufferingUpdate(bufferedPercent)
    }
  }

  override var surface: Surface? = null
  set(value) {
    field = value
    sendMessage(C.TRACK_TYPE_VIDEO, Renderer.MSG_SET_SURFACE, surface)
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
      sendMessage(C.TRACK_TYPE_AUDIO, Renderer.MSG_SET_VOLUME, requestedVolume)
    }

  override val playbackState: Int
    get() = exoPlayer.playbackState

  override var playbackSpeed: Float
    get() = exoPlayer.playbackParameters.speed
    set(value) {
      exoPlayer.setPlaybackParameters(PlaybackParameters(value, exoPlayer.playbackParameters.pitch))
    }

  override var playbackPitch: Float
    get() = exoPlayer.playbackParameters.pitch
    set(value) {
      exoPlayer.setPlaybackParameters(PlaybackParameters(exoPlayer.playbackParameters.speed, value))
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

      val currentWindowIndex = exoPlayer.currentWindowIndex
      val currentWindow = timeline.getWindow(currentWindowIndex, Timeline.Window())

      return WindowInfo(
          exoPlayer.previousWindowIndex,
          currentWindowIndex,
          exoPlayer.nextWindowIndex,
          currentWindow
      )
    }

  override val audioSessionId: Int
    get() = TODO("The DefaultAudioRenderListener keeps track of the audioSessionId")

  override val playing: Boolean
    get() = exoPlayer.isPlaying

  override var playWhenReady: Boolean
    get() = exoPlayer.playWhenReady
    set(playWhenReady) {
      exoPlayer.playWhenReady = playWhenReady
      config.wakeManager.stayAwake(playWhenReady)
    }

  private val renderers by lazy {
    config.renderProviders.values.flatMap { it.buildRenderers() }
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, state: Int) {
    reportPlayerState()
  }

  override fun onPlayerError(error: ExoPlaybackException) {
    listeners.forEach {
      it.onError(this, error)
    }
  }

  override fun setMediaUri(uri: Uri?) {
    val mediaSource = uri?.let {
      val attributes = MediaSourceBuilder.MediaSourceAttributes(
          config.context,
          it,
          config.handler,
          config.userAgentProvider.userAgent,
          config.bandwidthMeter.transferListener,
          drmSessionManagerProvider
      )

      config.mediaSourceProvider.generate(attributes)
    }

    setMediaSource(mediaSource)
  }

  override fun setMediaSource(source: MediaSource?) {
    mediaSource?.let {
      it.removeEventListener(config.analyticsCollector)
      config.analyticsCollector.resetForNewPlaylist()
    }

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
    config.coreListeners.metadataListener = listener
  }

  override fun setCaptionListener(listener: CaptionListener?) {
    config.coreListeners.captionListener = listener
  }

  override fun setVideoSizeListener(listener: VideoSizeListener?) {
    config.coreListeners.videoSizeListener = listener
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

    sendMessage(C.TRACK_TYPE_VIDEO, Renderer.MSG_SET_SURFACE, null)
  }

  override fun setAudioStreamType(streamType: Int) {
    val usage = Util.getAudioUsageForStreamType(streamType)
    val contentType = Util.getAudioContentTypeForStreamType(streamType)

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(usage)
        .setContentType(contentType)
        .build()

    sendMessage(C.TRACK_TYPE_AUDIO, Renderer.MSG_SET_AUDIO_ATTRIBUTES, audioAttributes)
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

    if (renderers.isNotEmpty()) {
      exoPlayer.stop()
    }

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

  override fun stop() {
    if (!stopped.getAndSet(true)) {
      exoPlayer.playWhenReady = false
      exoPlayer.stop()
    }
  }

  override fun release() {
    // TODO: there's no returning from cancel(), is that expected in release? (or should we revert to using the Repeater?)
    bufferTimer.cancel()
    listeners.clear()

    mediaSource?.removeEventListener(config.analyticsCollector)

    surface = null
    playWhenReady = false
    exoPlayer.release()
    config.wakeManager.stayAwake(false)
  }

  override fun seekTo(positionMs: Long) {
    seekTo(positionMs, false)
  }

  override fun seekTo(positionMs: Long, limitToCurrentWindow: Boolean) {
    config.analyticsCollector.notifySeekStarted()
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
    val maxWindowIndex = min(timeline.windowCount - 1, exoPlayer.currentWindowIndex)

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

  protected fun sendMessage(renderType: Int, messageType: Int, message: Any?) {
    renderers.forEach { renderer ->
      if (renderer.trackType == renderType) {
        exoPlayer.createMessage(renderer)
            .setType(messageType)
            .setPayload(message)
            .send()
      }
    }
  }

  private fun reportPlayerState() {
    val playWhenReady = exoPlayer.playWhenReady
    val playbackState = playbackState

    // Don't report duplicate states
    val newState = stateStore.getState(playWhenReady, playbackState)
    if (newState == stateStore.mostRecentState) {
      return
    }

    stateStore.setMostRecentState(playWhenReady, playbackState)
    val informSeekCompletion = stateStore.seekCompleted()

    listeners.forEach { listener ->
      listener.onStateChanged(playWhenReady, playbackState)

      if (informSeekCompletion) {
        listener.onSeekComplete()
      }
    }
  }
}