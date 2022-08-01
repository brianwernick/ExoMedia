package com.devbrackets.android.exomedia.core

import android.os.Handler
import android.os.Looper
import androidx.annotation.IntRange
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
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

  companion object {
    //The amount of time the current position can be off the duration to call the onCompletion listener
    private const val COMPLETED_DURATION_LEEWAY: Long = 1_000
  }

  private val delayedHandler = Handler(Looper.getMainLooper())

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

  override fun onStateChange(state: FallbackMediaPlayer.State) {
    when (state) {
      FallbackMediaPlayer.State.COMPLETED -> completionListener?.onCompletion()
      FallbackMediaPlayer.State.READY -> {
        if (!isPrepared) {
          notifyPreparedListener()
        }
      }
      else -> {}
    }
  }

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

  override fun onError(player: ExoMediaPlayer, e: Exception?) {
    muxNotifier.onMediaPlaybackEnded()
    muxNotifier.onExoPlayerError(player, e)
    notifyErrorListener(e)
  }

  override fun onStateChanged(playWhenReady: Boolean, playbackState: Int) {
    when (playbackState) {
      Player.STATE_READY -> {
        if (!isPrepared) {
          notifyPreparedListener()
        }
        if (playWhenReady) {
          //Updates the previewImage
          muxNotifier.onPreviewImageStateChanged(false)
        }
      }
      Player.STATE_IDLE -> {
        if (clearRequested) {
          //Clears the textureView when requested
          clearRequested = false
          surfaceEnvelopeRef.get()?.let {
            it.clearSurface()
            surfaceEnvelopeRef.clear()
          }
        }
      }
      Player.STATE_ENDED -> {
        muxNotifier.onMediaPlaybackEnded()

        if (!notifiedCompleted) {
          notifyCompletionListener()
        }
      }
    }
  }

  override fun onSeekComplete() {
    muxNotifier.onSeekComplete()
    seekCompletionListener?.onSeekComplete()
  }

  override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
    muxNotifier.onBufferUpdated(percent)
    bufferUpdateListener?.onBufferingUpdate(percent)
  }

  override fun onMetadata(metadata: Metadata) {
    metadataListener?.onMetadata(metadata)
  }

  override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
    muxNotifier.onVideoSizeChanged(width, height, unAppliedRotationDegrees, pixelWidthHeightRatio)
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
   * Sets weather the listener was notified when we became prepared.
   *
   * @param wasNotified True if the onPreparedListener was already notified
   */
  fun setNotifiedPrepared(wasNotified: Boolean) {
    isPrepared = wasNotified
    muxNotifier.onPreviewImageStateChanged(true)
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
    return errorListener?.onError(e) == true
  }

  private fun notifyPreparedListener() {
    isPrepared = true

    delayedHandler.post {
      performPreparedHandlerNotification()
    }
  }

  private fun performPreparedHandlerNotification() {
    muxNotifier.onPrepared()
    preparedListener?.onPrepared()
  }

  private fun notifyCompletionListener() {
    if (!muxNotifier.shouldNotifyCompletion(COMPLETED_DURATION_LEEWAY)) {
      return
    }

    notifiedCompleted = true

    delayedHandler.post {
      completionListener?.onCompletion()
    }
  }

  abstract class Notifier {
    open fun onSeekComplete() {
      //Purposefully left blank
    }

    fun onBufferUpdated(percent: Int) {
      //Purposefully left blank
    }

    open fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      //Purposefully left blank
    }

    open fun onPrepared() {
      //Purposefully left blank
    }

    open fun onPreviewImageStateChanged(toVisible: Boolean) {
      //Purposefully left blank
    }

    abstract fun shouldNotifyCompletion(endLeeway: Long): Boolean

    abstract fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?)

    abstract fun onMediaPlaybackEnded()
  }
}