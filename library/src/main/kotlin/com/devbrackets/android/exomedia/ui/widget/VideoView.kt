package com.devbrackets.android.exomedia.ui.widget

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewStub
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.audio.MediaItem
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.core.state.PlaybackStateListener
import com.devbrackets.android.exomedia.core.video.ExoVideoPlayer
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.core.video.layout.AspectRatioLayout
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.core.video.surface.SurfaceEnvelope
import com.devbrackets.android.exomedia.core.video.surface.SurfaceViewSurfaceEnvelope
import com.devbrackets.android.exomedia.core.video.surface.TextureViewSurfaceEnvelope
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.devbrackets.android.exomedia.ui.widget.attr.VideoViewAttributeParser
import com.devbrackets.android.exomedia.ui.widget.attr.VideoViewAttributes
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControls
import com.devbrackets.android.exomedia.util.StopWatch

/**
 * This is a support VideoView that will use the standard, MediaPlayer backed, VideoView
 * on devices that don't support the ExoPlayer; otherwise the ExoPlayer backed VideoView
 * will be used to provide better configurability and format support.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class VideoView : RelativeLayout, PlaybackStateListener {

  /**
   * Gets the preview ImageView for use with image loading libraries.
   *
   * @return the preview ImageView
   */
  val previewImageView: ImageView? by lazy { findViewById(R.id.exomedia_video_preview_image) }
  protected val aspectRatioLayout: AspectRatioLayout by lazy { findViewById(R.id.exomedia_video_ratio_layout) }

  protected val surface: View by lazy { findViewById(R.id.exomedia_surface_view) }
  protected val surfaceEnvelope by lazy {
    constructEnvelope(surface)
  }

  protected val videoPlayer: VideoPlayerApi by lazy { getApiImplementation() }

  /**
   * Gets the [MediaItem] currently used. This is specified by calling [setMedia]
   */
  var mediaItem: MediaItem? = null
    protected set

  protected var audioManager: AudioManager? = null
  protected var audioFocusHelper = AudioFocusHelper()

  /**
   * Sets the amount of time to change the return value from [.getCurrentPosition].
   * This value will be reset when a new audio item is selected.
   */
  var positionOffset: Long = 0
  protected var overriddenDuration: Long = -1

  protected var overridePosition = false
  protected var matchOverridePositionSpeed = true

  protected var overriddenPositionStopWatch = StopWatch()

  protected var muxNotifier = MuxNotifier()
  protected val listenerMux: ListenerMux by lazy {
    ListenerMux(muxNotifier).apply {
      setPlaybackStateListener(this@VideoView)
    }
  }
  protected lateinit var playerConfig: PlayerConfig

  protected var playbackListener: PlaybackStateListener? = null

  /**
   * ***WARNING:*** Use of this method may cause memory leaks.
   *
   * Enables or disables the automatic release when the VideoView is detached
   * from the window. Normally this is expected to release all resources used
   * by calling [.release].  If `releaseOnDetach` is disabled
   * then [.release] will need to be manually called.
   */
  var releaseOnDetachFromWindow = true

  /**
   * Enables or Disables automatic handling of audio focus. By default this is enabled
   * however in instances where a service handles playback of both audio and video it
   * is recommended to disable this and manually handle it in the service for consistency
   */
  var handleAudioFocus = true
    set(value) {
      audioFocusHelper.abandonFocus()
      field = value
    }

  /**
   * Retrieves the video controls being used by this view.
   * If the controls haven't been specified with [.setControls]
   * or through the XML attribute `useDefaultControls` this will return
   * null
   *
   * @return The video controls being used by this view or null
   */
  var videoControls: VideoControls? = null
    set(value) {
      if (field != value) {
        field?.onDetachedFromView(this)
      }

      field = value
      field?.onAttachedToView(this)
    }

  /**
   * Retrieves the current media volume
   *
   * @return The volume for the media
   */
  var volume: Float
    get() = videoPlayer.volume
    set(value) {
      videoPlayer.volume = value
    }

  /**
   * Returns if a video is currently in playback
   *
   * @return True if a video is playing
   */
  val isPlaying: Boolean
    get() = videoPlayer.isPlaying

  /**
   * Retrieves the duration of the current audio item.  This should only be called after
   * the item is prepared (see [.setOnPreparedListener]).
   * If [.overrideDuration] is set then that value will be returned.
   *
   * @return The millisecond duration of the video
   */
  val duration: Long
    get() = if (overriddenDuration >= 0) {
      overriddenDuration
    } else videoPlayer.duration

  /**
   * Retrieves the current position of the audio playback.  If an audio item is not currently
   * in playback then the value will be 0.  This should only be called after the item is
   * prepared (see [.setOnPreparedListener])
   *
   * @return The millisecond value for the current position
   */
  val currentPosition: Long
    get() = if (overridePosition) {
      positionOffset + overriddenPositionStopWatch.time
    } else positionOffset + videoPlayer.currentPosition

  /**
   * Retrieves the current buffer percent of the video.  If a video is not currently
   * prepared or buffering the value will be 0.  This should only be called after the video is
   * prepared (see [.setOnPreparedListener])
   *
   * @return The integer percent that is buffered [0, 100] inclusive
   */
  val bufferPercentage: Int
    get() = videoPlayer.bufferedPercent

  /**
   * Retrieves the information associated with the current [androidx.media3.common.Timeline.Window]
   * used by the ExoPlayer backed implementation. When the [android.media.MediaPlayer] backed
   * implementation is being used this will be null.
   *
   * @return The current Window information or null
   */
  val windowInfo: WindowInfo?
    get() = videoPlayer.windowInfo

  /**
   * Retrieves the current speed the media is playing at.
   *
   * @return The current playback speed
   */
  val playbackSpeed: Float
    get() = videoPlayer.playbackSpeed

  /**
   * Retrieves a list of available tracks to select from. Typically [.trackSelectionAvailable]
   * should be called before this.
   *
   * @return A list of available tracks associated with each track type
   */
  val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = videoPlayer.availableTracks

  /**
   * Returns a [Bitmap] representation of the current contents of the
   * view. If the surface isn't ready or we cannot access it for some reason then
   * `null` will be returned instead.
   *
   * **NOTE:** Only the `TextureView` implementations support getting the bitmap
   * meaning that if the backing implementation is a `SurfaceView` then the result
   * will always be `null`
   *
   * @return A [Bitmap] representation of the view or `null`
   */
  val bitmap: Bitmap?
    get() = (videoPlayer as? TextureView)?.bitmap

  constructor(context: Context) : super(context) {
    setup(context, null)
  }

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    setup(context, attrs)
  }

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    setup(context, attrs)
  }

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    setup(context, attrs)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (!isInEditMode && releaseOnDetachFromWindow) {
      release()
    }
  }

  override fun onPlaybackStateChange(state: PlaybackState) {
    videoControls?.onPlaybackStateChange(state)
    playbackListener?.onPlaybackStateChange(state)

    updatePreviewVisibility(state)
  }

  @Suppress("FoldInitializerAndIfToElvis")
  protected fun updatePreviewVisibility(state: PlaybackState) {
    val view = previewImageView
    if (view == null) {
      return
    }

    // Show when preparing
    if (state == PlaybackState.IDLE || state == PlaybackState.PREPARING) {
      view.visibility = View.VISIBLE
      return
    }

    // Hide when playback was started
    if (view.visibility == View.VISIBLE && state == PlaybackState.PLAYING) {
      view.visibility = View.GONE
    }
  }

  /**
   * Sets an image that will be visible only when the video is loading.
   *
   * @param drawable The drawable to use for the preview image
   */
  fun setPreviewImage(drawable: Drawable?) {
    previewImageView?.setImageDrawable(drawable)
  }

  /**
   * Sets an image that will be visible only when the video is loading.
   *
   * @param resourceId The resourceId representing the preview image
   */
  fun setPreviewImage(@DrawableRes resourceId: Int) {
    previewImageView?.setImageResource(resourceId)
  }

  /**
   * Sets an image that will be visible only when the video is loading.
   *
   * @param bitmap The bitmap to use for the preview image
   */
  fun setPreviewImage(bitmap: Bitmap?) {
    previewImageView?.setImageBitmap(bitmap)
  }

  /**
   * Sets an image that will be visible only when the video is loading.
   *
   * @param uri The Uri pointing to the preview image
   */
  fun setPreviewImage(uri: Uri?) {
    previewImageView?.setImageURI(uri)
  }

  /**
   * Sets the [Uri] to play the video from
   *
   * @param uri The video's [Uri]
   */
  fun setMedia(uri: Uri?) {
    val mediaItem = uri?.let {
      MediaItem(it, null)
    }

    videoPlayer.setMedia(mediaItem)
    this.mediaItem = mediaItem
  }

  /**
   * Sets the [MediaSource] to play the video from
   *
   * @param mediaSource [MediaSource] that should be used
   */
  fun setMedia(mediaSource: MediaSource?) {
    val mediaItem = mediaSource?.let {
      MediaItem(null, it)
    }

    videoPlayer.setMedia(mediaItem)
    this.mediaItem = mediaItem
  }

  /**
   * Sets the [DrmSessionManager] to use when handling DRM for media.
   * This should be specified before setting the video's URI
   *
   * NOTE: This will only configure DRM for the ExoPlayer implementation and not
   * the native (MediaPlayer) backed implementation.
   *
   * @param drmSessionManagerProvider The session manager provider to handle the DRM authorization
   */
  fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider?) {
    videoPlayer.drmSessionManagerProvider = drmSessionManagerProvider
  }

  /**
   * Moves the current video progress to the specified location.
   *
   * @param milliSeconds The time to move the playback to
   */
  fun seekTo(milliSeconds: Long) {
    videoPlayer.seekTo(milliSeconds)
  }

  /**
   * Starts the playback for the video specified in [.setVideoURI]
   * or [.setVideoPath].  This should be called after the VideoView is correctly
   * prepared (see [.setOnPreparedListener])
   */
  fun start() {
    if (!audioFocusHelper.requestFocus()) {
      return
    }

    videoPlayer.start()
    keepScreenOn = true
  }

  /**
   * Pauses the current video in playback, only abandoning the audio focus if
   * `transientFocusLoss` is `false`. Calling [pause] should
   * be used in most cases unless the audio focus is being handled manually
   *
   * @param transientFocusLoss `true` if the pause is temporary and the audio focus should be retained
   */
  @JvmOverloads
  fun pause(transientFocusLoss: Boolean = false) {
    if (!transientFocusLoss) {
      audioFocusHelper.abandonFocus()
    }

    videoPlayer.pause()
    keepScreenOn = false
  }

  /**
   * If the video has completed playback, calling `restart` will seek to the beginning of the video, and play it.
   *
   * @return `true` if the video was successfully restarted, otherwise `false`
   */
  fun restart(): Boolean {
    if (mediaItem == null) {
      return false
    }

    if (videoPlayer.restart()) {
      return true
    }

    return false
  }

  /**
   * If a video is currently in playback then the playback will be stopped
   */
  fun stop() {
    stopPlayback(true)
  }

  /**
   * Stops the current video playback and resets the listener states
   * so that we receive the callbacks for events like onPrepared
   */
  fun reset() {
    stop()
    setMedia(uri = null)
  }

  /**
   * Stops the playback and releases all resources attached to this
   * VideoView. This should not be called manually unless
   * [releaseOnDetachFromWindow] has been set.
   */
  fun release() {
    videoControls = null

    stop()
    overriddenPositionStopWatch.stop()

    videoPlayer.release()
  }

  /**
   * If a video is currently in playback then the playback will be suspended.
   * This is similar to the functionality provided by [release] with the exception
   * that the [videoControls] won't be detached.
   */
  fun suspend() {
    audioFocusHelper.abandonFocus()
    videoPlayer.release()
    keepScreenOn = false
  }

  /**
   * Setting this will override the duration that the item may actually be.  This method should
   * only be used when the item doesn't return the correct duration such as with audio streams.
   * This only overrides the current audio item.
   *
   * @param duration The duration for the current media item or &lt; 0 to disable
   */
  fun overrideDuration(duration: Long) {
    overriddenDuration = duration
  }

  /**
   * Restarts the audio position to the start if the position is being overridden (see [.overridePosition]).
   * This will be the value specified with [.setPositionOffset] or 0 if it hasn't been set.
   */
  fun restartOverridePosition() {
    overriddenPositionStopWatch.reset()
  }

  /**
   * Sets if the position should be overridden, allowing the time to be restarted at will.  This
   * is useful for streaming media where the media doesn't have breaks between songs.
   *
   * @param override True if the position should be overridden
   */
  fun overridePosition(override: Boolean) {
    if (override) {
      overriddenPositionStopWatch.start()
    } else {
      overriddenPositionStopWatch.stop()
    }

    overridePosition = override
  }

  /**
   * If set the overridden position will use the same playback rate as the
   * media in playback.
   *
   * @param match `true` to match the playback speed
   */
  fun setOverridePositionMatchesPlaybackSpeed(match: Boolean) {
    if (match == matchOverridePositionSpeed) {
      return
    }

    matchOverridePositionSpeed = match

    if (match) {
      overriddenPositionStopWatch.speedMultiplier = playbackSpeed
    } else {
      // Defaults to 1x when disabled
      overriddenPositionStopWatch.speedMultiplier = 1f
    }
  }

  /**
   * Sets the repeat mode for this MediaPlayer.
   * **Note:** This will only change the ExoPlayer implementation
   *
   * @param repeatMode The repeat mode to use
   */
  fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    videoPlayer.setRepeatMode(repeatMode)
  }

  /**
   * Sets the playback speed for this MediaPlayer.
   *
   * @param speed The speed to play the media back at
   * @return True if the speed was set
   */
  fun setPlaybackSpeed(speed: Float): Boolean {
    val wasSet = videoPlayer.setPlaybackSpeed(speed)
    if (wasSet && matchOverridePositionSpeed) {
      overriddenPositionStopWatch.speedMultiplier = speed
    }

    return wasSet
  }

  /**
   * Sets the caption listener for this MediaPlayer
   * Only the exoplayer implementation supports captions.
   *
   * @param listener The caption listener
   */
  fun setCaptionListener(listener: CaptionListener?) {
    videoPlayer.setCaptionListener(listener)
  }

  /**
   * Determines if the current video player implementation supports
   * track selection for audio or video tracks.
   *
   * @return True if tracks can be manually specified
   */
  fun trackSelectionAvailable(): Boolean {
    return videoPlayer.trackSelectionAvailable()
  }

  /**
   * Changes to the track with `trackIndex` for the specified
   * `trackType`
   *
   * @param trackType The type for the track to switch to the selected index
   * @param groupIndex The index for the group in the [TrackGroupArray] specified by the `trackType`
   * @param trackIndex The index for the track to switch to
   */
  fun setTrack(trackType: RendererType, groupIndex: Int, trackIndex: Int) {
    videoPlayer.setSelectedTrack(trackType, groupIndex, trackIndex)
  }

  fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int {
    return videoPlayer.getSelectedTrackIndex(type, groupIndex)
  }

  /**
   * Clear all selected tracks for the specified renderer.
   * @param type The renderer type
   */
  fun clearSelectedTracks(type: RendererType) {
    videoPlayer.clearSelectedTracks(type)
  }

  /**
   * Enables or disables the track associated with the `type`. Note, by default all
   * tracks are enabled
   *
   * @param type The [RendererType] to enable or disable the track for
   * @param enabled `true` if the track should be enabled.
   */
  fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    videoPlayer.setRendererEnabled(type, enabled)
  }

  /**
   * Return true if at least one renderer for the given type is enabled
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: RendererType): Boolean {
    return videoPlayer.isRendererEnabled(type)
  }

  /**
   * Sets how the video should be scaled in the view
   *
   * @param scaleType how to scale the videos
   */
  fun setScaleType(scaleType: ScaleType) {
    surfaceEnvelope.setScaleType(scaleType)
  }

  /**
   * Measures the underlying [VideoPlayerApi] using the video's aspect ratio if `true`
   *
   * @param enabled whether to measure using the video's aspect ratio or not
   */
  fun setMeasureBasedOnAspectRatioEnabled(enabled: Boolean) {
    aspectRatioLayout.honorAspectRatio = enabled
  }

  /**
   * Sets the rotation for the Video, this will apply the rotation on-top of the
   * specified rotation in the media file.
   *
   * **NOTE:**
   * Setting a manual video rotation is _not_ supported by the `SurfaceView`, this
   * means you will need to use the `TextureView` backing which can be applied by
   * adding `app:useTextureViewBacking="true"` to the `VideoView` definition in
   * XML.
   *
   * @param rotation The rotation to apply to the video
   */
  fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int) {
    surfaceEnvelope.setVideoRotation(rotation, true)
  }

  /**
   * Sets the listener to inform of VideoPlayer prepared events
   *
   * @param listener The listener
   */
  fun setOnPreparedListener(listener: OnPreparedListener?) {
    listenerMux.setOnPreparedListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer completion events
   *
   * @param listener The listener
   */
  fun setOnCompletionListener(listener: OnCompletionListener?) {
    listenerMux.setOnCompletionListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer buffer update events
   *
   * @param listener The listener
   */
  fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
    listenerMux.setOnBufferUpdateListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer seek completion events
   *
   * @param listener The listener
   */
  fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
    listenerMux.setOnSeekCompletionListener(listener)
  }

  /**
   * Sets the listener to inform of playback errors
   *
   * @param listener The listener
   */
  fun setOnErrorListener(listener: OnErrorListener?) {
    listenerMux.setOnErrorListener(listener)
  }

  /**
   * Sets the listener to inform of ID3 metadata updates
   *
   * @param listener The listener
   */
  fun setId3MetadataListener(listener: MetadataListener?) {
    listenerMux.setMetadataListener(listener)
  }

  /**
   * Sets the listener to inform of Analytics updates
   *
   * @param listener The listener to inform
   */
  fun setAnalyticsListener(listener: AnalyticsListener?) {
    listenerMux.setAnalyticsListener(listener)
  }

  /**
   * Sets the listener to inform of video size changes
   *
   * @param listener The listener
   */
  fun setOnVideoSizedChangedListener(listener: OnVideoSizeChangedListener?) {
    muxNotifier.videoSizeChangedListener = listener
  }

  /**
   * Sets the listener to inform of playback state changes. If only the current value
   * is needed then [getPlaybackState] can be used.
   *
   * @param listener The listener to inform of [PlaybackState] changes
   */
  fun setPlaybackStateListener(listener: PlaybackStateListener?) {
    playbackListener = listener
  }

  /**
   * Retrieves the current [PlaybackState] of this [VideoView]. Changes to this value
   * can also be listened to via the [setPlaybackStateListener].
   *
   * @return The current [PlaybackState] of this [VideoView]
   */
  fun getPlaybackState(): PlaybackState {
    return listenerMux.playbackState
  }

  protected fun constructEnvelope(surface: View): SurfaceEnvelope {
    return when (surface) {
      is SurfaceView -> SurfaceViewSurfaceEnvelope(surface, MatrixManager())
      is TextureView -> TextureViewSurfaceEnvelope(surface, MatrixManager())
      else -> {
        throw IllegalArgumentException("Provided surface must be either a SurfaceView or TextureView")
      }
    }
  }

  /**
   * Performs the functionality to setup the initial properties including
   * determining the backing implementation and reading xml attributes
   *
   * @param context The context to use for setting up the view
   * @param attrs The xml attributes associated with this instance
   */
  protected fun setup(context: Context, attrs: AttributeSet?) {
    if (isInEditMode) {
      return
    }

    val attributes = VideoViewAttributeParser().parse(context, attrs)
    audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    playerConfig = attributes.playerConfigProvider.getConfig(context)

    initView(context, attributes)
    postInit(attributes)
  }

  /**
   * Performs the initialization of the view including inflating the correct
   * backing layout, linking the implementation, and finding the necessary view
   * references.
   *
   * @param context The context for the initialization
   * @param attributes The attributes associated with this instance
   */
  protected fun initView(context: Context, attributes: VideoViewAttributes) {
    View.inflate(context, R.layout.exomedia_video_view_layout, this)

    // Inflates the correct surface
    findViewById<ViewStub>(R.id.video_view_surface_stub).apply {
      layoutResource = if (attributes.useTextureViewBacking) R.layout.exomedia_texture_view else R.layout.exomedia_surface_view
      inflate()
    }

    videoPlayer.setListenerMux(listenerMux)
  }

  /**
   * Handles any setup that needs to be performed after [.initView]
   * is performed.
   *
   * @param attributes The attributes associated with this instance
   */
  protected fun postInit(attributes: VideoViewAttributes) {
    attributes.scaleType?.let {
      setScaleType(it)
    }

    videoControls = attributes.videoControlsProvider.getControls(context)
    setMeasureBasedOnAspectRatioEnabled(attributes.measureBasedOnAspectRatio)
  }

  /**
   * Retrieves the API implementation to use to play media
   */
  fun getApiImplementation(): VideoPlayerApi {
    if (playerConfig.fallbackManager.useFallback()) {
      return playerConfig.fallbackManager.getFallbackVideoPlayer(context, surfaceEnvelope)
    }

    return ExoVideoPlayer(playerConfig, surfaceEnvelope)
  }

  /**
   * Performs the functionality to stop the progress polling, and stop any other
   * procedures from running that we no longer need.
   */
  protected fun onPlaybackEnded() {
    stopPlayback(false)
  }

  /**
   * Stops the video currently in playback, making sure to only clear the surface
   * when requested. This allows us to leave the last frame of a video intact when
   * it plays to completion while still clearing it when the user requests playback
   * to stop.
   *
   * @param clearSurface `true` if the surface should be cleared
   */
  protected fun stopPlayback(clearSurface: Boolean) {
    audioFocusHelper.abandonFocus()
    videoPlayer.stop(clearSurface)
    keepScreenOn = false
  }

  /**
   * A utility used to handle the audio focus for the [VideoView]
   * when enabled.
   */
  protected inner class AudioFocusHelper : AudioManager.OnAudioFocusChangeListener {
    private var startRequested = false
    private var pausedForLoss = false
    private var currentFocus = 0

    @TargetApi(Build.VERSION_CODES.O)
    private var lastFocusRequest: AudioFocusRequest? = null

    override fun onAudioFocusChange(focusChange: Int) {
      if (!handleAudioFocus || currentFocus == focusChange) {
        return
      }

      currentFocus = focusChange
      when (focusChange) {
        AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> if (startRequested || pausedForLoss) {
          start()
          startRequested = false
          pausedForLoss = false
        }
        AudioManager.AUDIOFOCUS_LOSS -> if (isPlaying) {
          pausedForLoss = true
          pause()
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isPlaying) {
          pausedForLoss = true
          pause(true)
        }
      }
    }

    /**
     * Requests to obtain the audio focus
     *
     * @return True if the focus was granted
     */
    fun requestFocus(): Boolean {
      if (!handleAudioFocus || currentFocus == AudioManager.AUDIOFOCUS_GAIN) {
        return true
      }

      if (audioManager == null) {
        return false
      }

      val status: Int
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val attributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
        lastFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(attributes)
          .build().also {
            status = audioManager!!.requestAudioFocus(it)
          }
      } else {
        @Suppress("DEPRECATION")
        status = audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
      }

      if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
        currentFocus = AudioManager.AUDIOFOCUS_GAIN
        return true
      }

      startRequested = true
      return false
    }

    /**
     * Requests the system to drop the audio focus
     *
     * @return True if the focus was lost
     */
    fun abandonFocus(): Boolean {
      if (!handleAudioFocus) {
        return true
      }

      if (audioManager == null) {
        return false
      }

      startRequested = false
      val status: Int
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        status = lastFocusRequest?.let { request ->
          audioManager!!.abandonAudioFocusRequest(request).also {
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == it) {
              // reset lastFocusRequest on success, there is no reason to try again
              lastFocusRequest = null
            }
          }
        } ?: AudioManager.AUDIOFOCUS_REQUEST_GRANTED
      } else {
        @Suppress("DEPRECATION")
        status = audioManager!!.abandonAudioFocus(this)
      }

      return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status
    }
  }

  protected inner class MuxNotifier : ListenerMux.Notifier() {
    var videoSizeChangedListener: OnVideoSizeChangedListener? = null

    override fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
      stop()
      exoMediaPlayer.forcePrepare()
    }

    override fun onMediaPlaybackEnded() {
      keepScreenOn = false
      onPlaybackEnded()
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      // NOTE: Android 5.0+ will always have an unAppliedRotationDegrees of 0 (ExoPlayer already handles it)
      surfaceEnvelope.setVideoRotation(unAppliedRotationDegrees, false)
      surfaceEnvelope.setVideoSize(width, height, pixelWidthHeightRatio)

      aspectRatioLayout.setAspectRatio(width, height, pixelWidthHeightRatio)
      videoSizeChangedListener?.onVideoSizeChanged(width, height, pixelWidthHeightRatio)
    }
  }
}