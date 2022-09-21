package com.devbrackets.android.exomedia.core

import android.os.Handler
import android.os.Looper
import androidx.annotation.IntRange
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.core.state.PlaybackStateListener
import com.devbrackets.android.exomedia.core.video.surface.SurfaceEnvelope
import com.devbrackets.android.exomedia.fallback.FallbackMediaPlayer
import com.devbrackets.android.exomedia.fallback.exception.NativeMediaPlaybackException
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl
import java.lang.ref.WeakReference

/**
 * An internal Listener that implements the listeners for the [ExoMediaPlayerImpl],
 * Android VideoView, and the Android MediaPlayer to output to the correct
 * error listeners.
 */
class ListenerMux(
  private val muxNotifier: Notifier,
  private val analyticsDelegate: AnalyticsDelegate = AnalyticsDelegate()
) :
  ExoPlayerListener,
  FallbackMediaPlayer.Listener,
  OnBufferUpdateListener,
  MetadataListener,
  AnalyticsListener by analyticsDelegate
{
  private val delayedHandler = Handler(Looper.getMainLooper())

  private var playbackStateListener: PlaybackStateListener? = null
  private var preparedListener: OnPreparedListener? = null
  private var completionListener: OnCompletionListener? = null
  private var bufferUpdateListener: OnBufferUpdateListener? = null
  private var seekCompletionListener: OnSeekCompletionListener? = null
  private var errorListener: OnErrorListener? = null
  private var metadataListener: MetadataListener? = null

  private var surfaceEnvelopeRef = WeakReference<SurfaceEnvelope>(null)

  /**
   * Retrieves if the player was prepared
   *
   * @return True if the player was prepared
   */
  var isPrepared = false
    private set
  private var notifiedCompleted = false
  private var clearRequested = false

  var playbackState = PlaybackState.IDLE
    private set

  override fun onBufferUpdate(mediaPlayer: FallbackMediaPlayer, percent: Int) {
    onBufferingUpdate(percent)
  }

  override fun onSeekComplete(mediaPlayer: FallbackMediaPlayer) {
    seekCompletionListener?.onSeekComplete()
  }

  override fun onError(mediaPlayer: FallbackMediaPlayer, what: Int, extra: Int): Boolean {
    return notifyErrorListener(NativeMediaPlaybackException(what, extra))
  }

  override fun onVideoSizeChanged(mediaPlayer: FallbackMediaPlayer, width: Int, height: Int) {
    muxNotifier.onVideoSizeChanged(width, height, 0, 1f)
  }

  override fun onPlaybackStateChange(state: PlaybackState) {
    playbackState = state
    playbackStateListener?.onPlaybackStateChange(state)

    notifyPreparedListener(state)

    when (state) {
      PlaybackState.IDLE -> {
        if (clearRequested) {
          clearRequested = false
          surfaceEnvelopeRef.get()?.let {
            it.clearSurface()
            surfaceEnvelopeRef.clear()
          }
        }
      }
      PlaybackState.COMPLETED -> {
        notifyCompletionListener()
      }
      PlaybackState.STOPPED, PlaybackState.RELEASED -> {
        muxNotifier.onMediaPlaybackEnded()
      }
      else -> {}
    }
  }

  override fun onError(player: ExoMediaPlayer, e: Exception?) {
    muxNotifier.onMediaPlaybackEnded()
    muxNotifier.onExoPlayerError(player, e)
    notifyErrorListener(e)
  }

  override fun onSeekComplete() {
    seekCompletionListener?.onSeekComplete()
  }

  override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
    muxNotifier.onVideoSizeChanged(width, height, unAppliedRotationDegrees, pixelWidthHeightRatio)
  }

  override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
    bufferUpdateListener?.onBufferingUpdate(percent)
  }

  override fun onMetadata(metadata: Metadata) {
    metadataListener?.onMetadata(metadata)
  }

  /**
   * Specifies the surface to clear when the playback reaches an appropriate state.
   * Once the [SurfaceEnvelope] is cleared, the reference will be removed
   *
   * @param surface The [SurfaceEnvelope] to clear when the playback reaches an appropriate state
   */
  fun clearSurfaceWhenReady(surface: SurfaceEnvelope?) {
    clearRequested = true
    surfaceEnvelopeRef = WeakReference(surface)
  }

  /**
   * Sets the listener to inform of VideoPlayer prepared events
   *
   * @param listener The listener to inform
   */
  fun setOnPreparedListener(listener: OnPreparedListener?) {
    preparedListener = listener
  }

  /**
   * Sets the listener to inform of VideoPlayer completion events
   *
   * @param listener The listener to inform
   */
  fun setOnCompletionListener(listener: OnCompletionListener?) {
    completionListener = listener
  }

  /**
   * Sets the listener to inform of buffering updates
   *
   * @param listener The listener to inform
   */
  fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
    bufferUpdateListener = listener
  }

  /**
   * Sets the listener to inform of VideoPlayer seek completion events
   *
   * @param listener The listener to inform
   */
  fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
    seekCompletionListener = listener
  }

  /**
   * Sets the listener to inform of playback errors
   *
   * @param listener The listener to inform
   */
  fun setOnErrorListener(listener: OnErrorListener?) {
    errorListener = listener
  }

  /**
   * Sets the listener to inform of ID3 metadata updates
   *
   * @param listener The listener to inform
   */
  fun setMetadataListener(listener: MetadataListener?) {
    metadataListener = listener
  }

  /**
   * Sets the listener to inform of Analytics updates
   *
   * @param listener The listener to inform
   */
  fun setAnalyticsListener(listener: AnalyticsListener?) {
   analyticsDelegate.listener = listener
  }

  /**
   * Sets the listener to inform of playback state changes
   *
   * @param listener The listener to inform
   */
  fun setPlaybackStateListener(listener: PlaybackStateListener?) {
    playbackStateListener = listener
  }

  /**
   * Sets weather the listener was notified when we became prepared.
   *
   * @param wasNotified True if the onPreparedListener was already notified
   */
  fun setNotifiedPrepared(wasNotified: Boolean) {
    isPrepared = wasNotified
  }

  /**
   * Sets weather the listener was notified when the playback was completed
   * (played through the end).
   *
   * @param wasNotified True if the onCompletionListener was already notified
   */
  fun setNotifiedCompleted(wasNotified: Boolean) {
    notifiedCompleted = wasNotified
  }

  private fun notifyErrorListener(e: Exception?): Boolean {
    return (errorListener?.onError(e) == true).also {
      muxNotifier.onMediaPlaybackEnded()
    }
  }

  private fun notifyPreparedListener(state: PlaybackState) {
    if (isPrepared) {
      return
    }

    when (state) {
      PlaybackState.READY, PlaybackState.PLAYING, PlaybackState.PAUSED -> {
        isPrepared = true

        delayedHandler.post {
          preparedListener?.onPrepared()
        }
      }
      else -> {}
    }
  }

  private fun notifyCompletionListener() {
    completionListener?.onCompletion()
    muxNotifier.onMediaPlaybackEnded()
  }

  abstract class Notifier {
    open fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      //Purposefully left blank
    }

    abstract fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?)

    abstract fun onMediaPlaybackEnded()
  }
}