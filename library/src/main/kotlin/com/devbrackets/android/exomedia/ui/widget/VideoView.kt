/*
 * Copyright (C) 2016 - 2021 ExoMedia Contributors
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
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.video.ExoVideoPlayer
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.core.video.surface.VideoSurface
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.ui.widget.attr.VideoViewAttributeParser
import com.devbrackets.android.exomedia.ui.widget.attr.VideoViewAttributes
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControls
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsLeanback
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsMobile
import com.devbrackets.android.exomedia.util.StopWatch
import com.devbrackets.android.exomedia.util.isDeviceTV
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import java.lang.IllegalArgumentException

/**
 * This is a support VideoView that will use the standard VideoView on devices below
 * JellyBean.  On devices with JellyBean and up we will use the ExoPlayer in order to
 * better support HLS streaming and full 1080p video resolutions which the VideoView
 * struggles with, and in some cases crashes.
 *
 *
 * To an external user this view should have the same APIs used with the standard VideoView
 * to help with quick implementations.
 *
 * TODO why doesn't this extend VideoPlayerApi?
 * TODO: standardize naming of exposed methods with the AudioPlayer (e.g. setMediaUri instead of setVideoUri)
 */
open class VideoView : RelativeLayout {

  /**
   * Gets the preview ImageView for use with image loading libraries.
   *
   * @return the preview ImageView
   */
  val previewImageView: ImageView? by lazy { findViewById(R.id.exomedia_video_preview_image) }
  protected val surface: View by lazy { findViewById(R.id.exomedia_surface_view) }

  protected val videoPlayer: VideoPlayerApi by lazy { getApiImplementation() }

  /**
   * Retrieves the current Video URI.  If this hasn't been set with [.setVideoURI]
   * or [.setVideoPath] then null will be returned.
   *
   * @return The current video URI or null
   */
  var videoUri: Uri? = null
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
  protected val listenerMux: ListenerMux by lazy { ListenerMux(muxNotifier) }
  protected lateinit var playerConfig: PlayerConfig

  /**
   * ***WARNING:*** Use of this method may cause memory leaks.
   *
   * Enables or disables the automatic release when the VideoView is detached
   * from the window.  Normally this is expected to release all resources used
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
   * @return The video controls being used by this view or null
   */
  var videoControls: VideoControls? = null
    set(value) {
      if (field != value) {
        field?.onDetachedFromView(this)
      }

      field = value
      field?.onAttachedToView(this)

      //Sets the onTouch repeatListener to show the controls
      setOnTouchListener(field?.let { TouchListener(context) })
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
   * Retrieves the information associated with the current [com.google.android.exoplayer2.Timeline.Window]
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
   * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
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

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    setup(context, attrs)
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    setup(context, attrs)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (!isInEditMode && releaseOnDetachFromWindow) {
      release()
    }
  }

  override fun setOnTouchListener(listener: OnTouchListener?) {
    surface.setOnTouchListener(listener)
    super.setOnTouchListener(listener)
  }

  /**
   * Stops the playback and releases all resources attached to this
   * VideoView.  This should not be called manually unless
   * [.setReleaseOnDetachFromWindow] has been set.
   */
  fun release() {
    videoControls?.onDetachedFromView(this)
    videoControls = null

    stopPlayback()
    overriddenPositionStopWatch.stop()

    videoPlayer.release()
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
   * Requests the [DefaultVideoControls] to become visible.  This should only be called after
   * [setControls].
   */
  fun showControls() {
    videoControls?.show()

    if (isPlaying) {
      videoControls?.hide(true)
    }
  }

  /**
   * Sets the Uri location for the video to play
   *
   * @param uri The video's Uri
   */
  fun setVideoURI(uri: Uri?) {
    videoUri = uri
    videoPlayer.setMedia(uri)
    videoControls?.showLoading(true)
  }

  /**
   * Sets the Uri location for the video to play
   *
   * @param uri The video's Uri
   * @param mediaSource MediaSource that should be used
   */
  fun setVideoURI(uri: Uri?, mediaSource: MediaSource?) {
    videoUri = uri
    videoPlayer.setMedia(uri, mediaSource)
    videoControls?.showLoading(true)
  }

  /**
   * Sets the path to the video.  This path can be a web address (e.g. http://) or
   * an absolute local path (e.g. file://)
   *
   * @param path The path to the video
   */
  fun setVideoPath(path: String) {
    setVideoURI(Uri.parse(path))
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
   * Stops the current video playback and resets the repeatListener states
   * so that we receive the callbacks for events like onPrepared
   */
  fun reset() {
    stopPlayback()
    setVideoURI(null)
  }

  /**
   * Moves the current video progress to the specified location.
   *
   * @param milliSeconds The time to move the playback to
   */
  fun seekTo(milliSeconds: Long) {
    videoControls?.showLoading(false)
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

    videoControls?.updatePlaybackState(true)
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

    videoControls?.updatePlaybackState(false)
  }

  /**
   * If a video is currently in playback then the playback will be stopped
   */
  fun stopPlayback() {
    stopPlayback(true)
  }

  /**
   * If the video has completed playback, calling `restart` will seek to the beginning of the video, and play it.
   *
   * @return `true` if the video was successfully restarted, otherwise `false`
   */
  fun restart(): Boolean {
    if (videoUri == null) {
      return false
    }

    if (videoPlayer.restart()) {
      videoControls?.showLoading(true)
      return true
    } else {
      return false
    }
  }

  /**
   * If a video is currently in playback then the playback will be suspended
   * TODO: rename to release? Internally we call release on the videoPlayer which
   *       means we won't be able to start playback again with this instance
   */
  fun suspend() {
    audioFocusHelper.abandonFocus()
    videoPlayer.release()
    keepScreenOn = false

    videoControls?.updatePlaybackState(false)
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
    if (match != matchOverridePositionSpeed) {
      matchOverridePositionSpeed = match
      if (match) {
        overriddenPositionStopWatch.speedMultiplier = playbackSpeed
      } else {
        // Defaults to 1x when disabled
        overriddenPositionStopWatch.speedMultiplier = 1f
      }
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
   * Sets the caption repeatListener for this MediaPlayer
   * Only the exoplayer implementation supports captions.
   *
   * @param listener The caption repeatListener
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
   * @param type The [com.devbrackets.android.exomedia.ExoMedia.RendererType] to enable or disable the track for
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
    (surface as VideoSurface).scaleType = scaleType
  }

  /**
   * Measures the underlying [VideoPlayerApi] using the video's aspect ratio if `true`
   *
   * @param measureBasedOnAspectRatioEnabled whether to measure using the video's aspect ratio or not
   */
  fun setMeasureBasedOnAspectRatioEnabled(measureBasedOnAspectRatioEnabled: Boolean) {
    (surface as VideoSurface).setMeasureBasedOnAspectRatioEnabled(measureBasedOnAspectRatioEnabled)
  }

  /**
   * Sets the rotation for the Video
   *
   * @param rotation The rotation to apply to the video
   */
  fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int) {
    (surface as VideoSurface).setVideoRotation(rotation, true)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer prepared events
   *
   * @param listener The repeatListener
   */
  fun setOnPreparedListener(listener: OnPreparedListener?) {
    listenerMux.setOnPreparedListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer completion events
   *
   * @param listener The repeatListener
   */
  fun setOnCompletionListener(listener: OnCompletionListener?) {
    listenerMux.setOnCompletionListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer buffer update events
   *
   * @param listener The repeatListener
   */
  fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
    listenerMux.setOnBufferUpdateListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer seek completion events
   *
   * @param listener The repeatListener
   */
  fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
    listenerMux.setOnSeekCompletionListener(listener)
  }

  /**
   * Sets the repeatListener to inform of playback errors
   *
   * @param listener The repeatListener
   */
  fun setOnErrorListener(listener: OnErrorListener?) {
    listenerMux.setOnErrorListener(listener)
  }

  /**
   * Sets the repeatListener to inform of ID3 metadata updates
   *
   * @param listener The repeatListener
   */
  fun setId3MetadataListener(listener: MetadataListener?) {
    listenerMux.setMetadataListener(listener)
  }

  /**
   * Sets the repeatListener to inform of Analytics updates
   *
   * @param listener The repeatListener to inform
   */
  fun setAnalyticsListener(listener: AnalyticsListener?) {
    listenerMux.setAnalyticsListener(listener)
  }

  /**
   * Sets the repeatListener to inform of video size changes
   *
   * @param listener The repeatListener
   */
  fun setOnVideoSizedChangedListener(listener: OnVideoSizeChangedListener?) {
    muxNotifier.videoSizeChangedListener = listener
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
      layoutResource = if (attributes.useTextureViewBacking) R.layout.exomedia_resizing_texture_view else R.layout.exomedia_resizing_surface_view
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
    if (attributes.useDefaultControls) {
      videoControls = if (context.isDeviceTV()) VideoControlsLeanback(context) else VideoControlsMobile(context)
    }

    attributes.scaleType?.let {
      setScaleType(it)
    }

    attributes.measureBasedOnAspectRatio?.let {
      setMeasureBasedOnAspectRatioEnabled(it)
    }
  }

  /**
   * Retrieves the API implementation to use to play media
   */
  fun getApiImplementation(): VideoPlayerApi {
    if (surface !is VideoSurface) {
      throw IllegalArgumentException("Provided surface must extend ClearableSurface")
    }

    val videoSurface = surface as VideoSurface
    return if (playerConfig.fallbackManager.useFallback()) {
      playerConfig.fallbackManager.getFallbackVideoPlayer(context, videoSurface)
    } else {
      ExoVideoPlayer(playerConfig, videoSurface)
    }
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

    videoControls?.updatePlaybackState(false)
  }

  /**
   * A utility used to handle the audio focus for the [VideoView]
   * when enabled.
   */
  protected inner class AudioFocusHelper : AudioManager.OnAudioFocusChangeListener {
    protected var startRequested = false
    protected var pausedForLoss = false
    protected var currentFocus = 0

    @TargetApi(Build.VERSION_CODES.O)
    protected var lastFocusRequest: AudioFocusRequest? = null

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
        status = audioManager!!.abandonAudioFocus(this)
      }

      return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status
    }
  }

  protected inner class MuxNotifier : ListenerMux.Notifier() {
    var videoSizeChangedListener: OnVideoSizeChangedListener? = null

    override fun shouldNotifyCompletion(endLeeway: Long): Boolean {
      val position = currentPosition
      val duration = duration
      return position > 0 && duration > 0 && position + endLeeway >= duration
    }

    override fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
      stopPlayback()
      exoMediaPlayer.forcePrepare()
    }

    override fun onMediaPlaybackEnded() {
      keepScreenOn = false
      onPlaybackEnded()
    }

    override fun onSeekComplete() {
      videoControls?.finishLoading()
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      //NOTE: Android 5.0+ will always have an unAppliedRotationDegrees of 0 (ExoPlayer already handles it)
      (surface as VideoSurface).setVideoRotation(unAppliedRotationDegrees, false)
      (surface as VideoSurface).onVideoSizeChanged(width, height, pixelWidthHeightRatio)

      videoSizeChangedListener?.onVideoSizeChanged(width, height, pixelWidthHeightRatio)
    }

    override fun onPrepared() {
      videoControls?.setDuration(duration)
      videoControls?.finishLoading()
    }

    override fun onPreviewImageStateChanged(toVisible: Boolean) {
      previewImageView?.visibility = if (toVisible) View.VISIBLE else View.GONE
    }
  }

  /**
   * Monitors the view click events to show and hide the video controls if they have been specified.
   * TODO: this should probably be located in the controls
   */
  protected inner class TouchListener(context: Context) : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener {
    protected val gestureDetector by lazy {
      GestureDetector(context, this)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
      gestureDetector.onTouchEvent(event)
      return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
      // Toggles between hiding and showing the controls
      if (videoControls?.isVisible == true) {
        videoControls?.hide(false)
      } else {
        showControls()
      }

      return true
    }
  }

}