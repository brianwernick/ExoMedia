/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.VideoViewApi
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView
import com.devbrackets.android.exomedia.core.video.mp.NativeTextureVideoView
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.util.DeviceUtil
import com.devbrackets.android.exomedia.util.StopWatch
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * This is a support VideoView that will use the standard VideoView on devices below
 * JellyBean.  On devices with JellyBean and up we will use the ExoPlayer in order to
 * better support HLS streaming and full 1080p video resolutions which the VideoView
 * struggles with, and in some cases crashes.
 *
 *
 * To an external user this view should have the same APIs used with the standard VideoView
 * to help with quick implementations.
 */
open class VideoView : RelativeLayout {

    /**
     * Gets the preview ImageView for use with image loading libraries.
     *
     * @return the preview ImageView
     */
    val previewImageView: ImageView? by lazy { findViewById<ImageView>(R.id.exomedia_video_preview_image) }
    protected val videoViewImpl: VideoViewApi by lazy { findViewById<View>(R.id.exomedia_video_view) as VideoViewApi }

    /**
     * Retrieves the current Video URI.  If this hasn't been set with [.setVideoURI]
     * or [.setVideoPath] then null will be returned.
     *
     * @return The current video URI or null
     */
    var videoUri: Uri? = null
        protected set
    protected var deviceUtil = DeviceUtil()

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
    var videoControls: VideoControlsCore? = null
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
        get() = videoViewImpl.volume
        set(value) { videoViewImpl.volume = value }

    /**
     * Returns if a video is currently in playback
     *
     * @return True if a video is playing
     */
    val isPlaying: Boolean
        get() = videoViewImpl.isPlaying

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
        } else videoViewImpl.duration

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
        } else positionOffset + videoViewImpl.currentPosition

    /**
     * Retrieves the current buffer percent of the video.  If a video is not currently
     * prepared or buffering the value will be 0.  This should only be called after the video is
     * prepared (see [.setOnPreparedListener])
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    val bufferPercentage: Int
        get() = videoViewImpl.bufferedPercent

    /**
     * Retrieves the information associated with the current [com.google.android.exoplayer2.Timeline.Window]
     * used by the ExoPlayer backed implementation. When the [android.media.MediaPlayer] backed
     * implementation is being used this will be null.
     *
     * @return The current Window information or null
     */
    val windowInfo: WindowInfo?
        get() = videoViewImpl.windowInfo

    /**
     * Retrieves the current speed the media is playing at.
     *
     * @return The current playback speed
     */
    val playbackSpeed: Float
        get() = videoViewImpl.playbackSpeed

    /**
     * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
        get() = videoViewImpl.availableTracks

    /**
     * Returns a [Bitmap] representation of the current contents of the
     * view. If the surface isn't ready or we cannot access it for some reason then
     * `null` will be returned instead.
     *
     *
     * **NOTE:** Only the `TextureView` implementations support getting the bitmap
     * meaning that if the backing implementation is a `SurfaceView` then the result
     * will always be `null`
     *
     * @return A [Bitmap] representation of the view or `null`
     */
    val bitmap: Bitmap?
        get() = (videoViewImpl as? TextureView)?.bitmap

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

    override fun setOnTouchListener(listener: View.OnTouchListener?) {
        videoViewImpl.setOnTouchListener(listener)
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

        videoViewImpl.release()
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
     * Requests the [VideoControls] to become visible.  This should only be called after
     * [.setControls].
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
        videoViewImpl.setVideoUri(uri)
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
        videoViewImpl.setVideoUri(uri, mediaSource)
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
     * Sets the [MediaDrmCallback] to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br></br>
     * **NOTE:** DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    fun setDrmCallback(drmCallback: MediaDrmCallback?) {
        videoViewImpl.setDrmCallback(drmCallback)
    }

    /**
     * Sets the volume level for devices that support
     * the ExoPlayer (JellyBean or greater).
     *
     * @param volume The volume range [0.0 - 1.0]
     * @return True if the volume was set
     */
    fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean {
        return videoViewImpl.setVolume(volume)
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
        videoViewImpl.seekTo(milliSeconds)
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

        videoViewImpl.start()
        keepScreenOn = true

        videoControls?.updatePlaybackState(true)
    }

    /**
     * Pauses the current video in playback, only abandoning the audio focus if
     * `transientFocusLoss` is `false`. Calling [.pause] should
     * be used in most cases unless the audio focus is being handled manually
     *
     * @param transientFocusLoss `true` if the pause is temporary and the audio focus should be retained
     */
    @JvmOverloads
    fun pause(transientFocusLoss: Boolean = false) {
        if (!transientFocusLoss) {
            audioFocusHelper.abandonFocus()
        }

        videoViewImpl.pause()
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

        if (videoViewImpl.restart()) {
            videoControls?.showLoading(true)
            return true
        } else {
            return false
        }
    }

    /**
     * If a video is currently in playback then the playback will be suspended
     */
    fun suspend() {
        audioFocusHelper.abandonFocus()
        videoViewImpl.suspend()
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
        videoViewImpl.setRepeatMode(repeatMode)
    }

    /**
     * Sets the playback speed for this MediaPlayer.
     *
     * @param speed The speed to play the media back at
     * @return True if the speed was set
     */
    fun setPlaybackSpeed(speed: Float): Boolean {
        val wasSet = videoViewImpl.setPlaybackSpeed(speed)
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
        videoViewImpl.setCaptionListener(listener)
    }

    /**
     * Determines if the current video player implementation supports
     * track selection for audio or video tracks.
     *
     * @return True if tracks can be manually specified
     */
    fun trackSelectionAvailable(): Boolean {
        return videoViewImpl.trackSelectionAvailable()
    }

    /**
     * Changes to the track with `trackIndex` for the specified
     * `trackType`
     *
     * @param trackType The type for the track to switch to the selected index
     * @param groupIndex The index for the group in the [TrackGroupArray] specified by the `trackType`
     * @param trackIndex The index for the track to switch to
     */
    fun setTrack(trackType: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
        videoViewImpl.setTrack(trackType, groupIndex, trackIndex)
    }

    fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
        return videoViewImpl.getSelectedTrackIndex(type, groupIndex)
    }

    /**
     * Clear all selected tracks for the specified renderer.
     * @param type The renderer type
     */
    fun clearSelectedTracks(type: ExoMedia.RendererType) {
        videoViewImpl.clearSelectedTracks(type)
    }

    /**
     * Enables or disables the track associated with the `type`. Note, by default all
     * tracks are enabled
     *
     * @param type The [com.devbrackets.android.exomedia.ExoMedia.RendererType] to enable or disable the track for
     * @param enabled `true` if the track should be enabled.
     */
    fun setRendererEnabled(type: ExoMedia.RendererType, enabled: Boolean) {
        videoViewImpl.setRendererEnabled(type, enabled)
    }

    /**
     * Return true if at least one renderer for the given type is enabled
     * @param type The renderer type
     * @return true if at least one renderer for the given type is enabled
     */
    fun isRendererEnabled(type: ExoMedia.RendererType): Boolean {
        return videoViewImpl.isRendererEnabled(type)
    }

    /**
     * Sets how the video should be scaled in the view
     *
     * @param scaleType how to scale the videos
     */
    fun setScaleType(scaleType: ScaleType) {
        videoViewImpl.scaleType = scaleType
    }

    /**
     * Measures the underlying [VideoViewApi] using the video's aspect ratio if `true`
     *
     * @param measureBasedOnAspectRatioEnabled whether to measure using the video's aspect ratio or not
     */
    fun setMeasureBasedOnAspectRatioEnabled(measureBasedOnAspectRatioEnabled: Boolean) {
        videoViewImpl.setMeasureBasedOnAspectRatioEnabled(measureBasedOnAspectRatioEnabled)
    }

    /**
     * Sets the rotation for the Video
     *
     * @param rotation The rotation to apply to the video
     */
    fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int) {
        videoViewImpl.setVideoRotation(rotation, true)
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

        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val attributeContainer = AttributeContainer(context, attrs)
        initView(context, attributeContainer)
        postInit(attributeContainer)
    }

    /**
     * Performs the initialization of the view including inflating the correct
     * backing layout, linking the implementation, and finding the necessary view
     * references.
     *
     * @param context The context for the initialization
     * @param attributeContainer The attributes associated with this instance
     */
    protected fun initView(context: Context, attributeContainer: AttributeContainer) {
        inflateVideoView(context, attributeContainer)

        videoViewImpl.setListenerMux(listenerMux)
    }

    /**
     * Handles any setup that needs to be performed after [.initView]
     * is performed.
     *
     * @param attributeContainer The attributes associated with this instance
     */
    protected fun postInit(attributeContainer: AttributeContainer) {
        if (attributeContainer.useDefaultControls) {
            videoControls = if (deviceUtil.isDeviceTV(context)) VideoControlsLeanback(context) else VideoControlsMobile(context)
        }

        attributeContainer.scaleType?.let {
            setScaleType(it)
        }

        attributeContainer.measureBasedOnAspectRatio?.let {
            setMeasureBasedOnAspectRatioEnabled(it)
        }
    }

    /**
     * Inflates the video view layout, replacing the [ViewStub] with the
     * correct backing implementation.
     *
     * @param context The context to use for inflating the correct video view
     * @param attributeContainer The attributes for retrieving custom backing implementations.
     */
    protected fun inflateVideoView(context: Context, attributeContainer: AttributeContainer) {
        View.inflate(context, R.layout.exomedia_video_view_layout, this)
        val videoViewStub = findViewById<ViewStub>(R.id.video_view_api_impl_stub)

        videoViewStub.layoutResource = getVideoViewApiImplementation(context, attributeContainer)
        videoViewStub.inflate()
    }

    /**
     * Retrieves the layout resource to use for the backing video view implementation.  By
     * default this uses the Android [android.widget.VideoView] on legacy devices with
     * APIs below Jellybean (16) or that don't pass the Compatibility Test Suite [CTS] via
     * [NativeTextureVideoView], and an ExoPlayer backed video view on the remaining devices via
     * [ExoTextureVideoView].
     *
     *
     * In the rare cases that the default implementations need to be extended, or replaced, the
     * user can override the value with the attributes `videoViewApiImplLegacy`
     * and `videoViewApiImpl`.
     *
     *
     * **NOTE:** overriding the default implementations may cause inconsistencies and isn't
     * recommended.
     *
     * @param context The Context to use when retrieving the backing video view implementation
     * @param attributeContainer The attributes to use for finding overridden video view implementations
     * @return The layout resource for the backing implementation on the current device
     */
    @LayoutRes
    protected fun getVideoViewApiImplementation(context: Context, attributeContainer: AttributeContainer): Int {
        val useLegacy = !deviceUtil.supportsExoPlayer(context)
        return if (useLegacy) attributeContainer.apiImplLegacyResourceId else attributeContainer.apiImplResourceId
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
        videoViewImpl.stopPlayback(clearSurface)
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
            videoViewImpl.setVideoRotation(unAppliedRotationDegrees, false)
            videoViewImpl.onVideoSizeChanged(width, height, pixelWidthHeightRatio)

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

    /**
     * A simple class that will retrieve the attributes and provide a simplified
     * interaction than passing around the [AttributeSet]
     */
    protected inner class AttributeContainer
    /**
     * Reads the attributes associated with this view, setting any values found
     *
     * @param context The context to retrieve the styled attributes with
     * @param attrs The [AttributeSet] to retrieve the values from
     */
    (context: Context, attrs: AttributeSet?) {
        /**
         * Specifies if the [VideoControls] should be added to the view.  These
         * can be added through source code with [.setControls]
         */
        var useDefaultControls = false

        /**
         * Specifies if the [VideoViewApi] implementations should use the [android.view.TextureView]
         * implementations.  If this is false then the implementations will be based on
         * the [android.view.SurfaceView]
         */
        var useTextureViewBacking = false

        /**
         * The resource id that points to a custom implementation for the `ExoPlayer`
         * backed [VideoViewApi]
         */
        var apiImplResourceId = R.layout.exomedia_default_exo_texture_video_view

        /**
         * The resource id that points to a custom implementation for the Android [android.media.MediaPlayer]
         * backed [VideoViewApi].  This will only be used on devices that do not support the
         * `ExoPlayer` (see [DeviceUtil.supportsExoPlayer] for details)
         */
        var apiImplLegacyResourceId = R.layout.exomedia_default_native_texture_video_view

        /**
         * Specifies the scale that the [VideoView] should use. If this is `null`
         * then the default value from the [com.devbrackets.android.exomedia.core.video.scale.MatrixManager]
         * will be used.
         */
        var scaleType: ScaleType? = null

        /**
         * Specifies if the [VideoView] should be measured based on the aspect ratio. Because
         * the default value is different between the [com.devbrackets.android.exomedia.core.video.ResizingSurfaceView]
         * and [com.devbrackets.android.exomedia.core.video.ResizingTextureView] this will be `null`
         * when not specified.
         */
        var measureBasedOnAspectRatio: Boolean? = null

        init {
            context.withStyledAttributes(attrs, R.styleable.VideoView) { typedArray ->
                useDefaultControls = typedArray.getBoolean(R.styleable.VideoView_useDefaultControls, useDefaultControls)
                useTextureViewBacking = typedArray.getBoolean(R.styleable.VideoView_useTextureViewBacking, useTextureViewBacking)

                if (typedArray.hasValue(R.styleable.VideoView_videoScale)) {
                    scaleType = ScaleType.fromOrdinal(typedArray.getInt(R.styleable.VideoView_videoScale, -1))
                }

                if (typedArray.hasValue(R.styleable.VideoView_measureBasedOnAspectRatio)) {
                    measureBasedOnAspectRatio = typedArray.getBoolean(R.styleable.VideoView_measureBasedOnAspectRatio, false)
                }

                //Resets the default implementations based on useTextureViewBacking
                apiImplResourceId = if (useTextureViewBacking) R.layout.exomedia_default_exo_texture_video_view else R.layout.exomedia_default_exo_surface_video_view
                apiImplLegacyResourceId = if (useTextureViewBacking) R.layout.exomedia_default_native_texture_video_view else R.layout.exomedia_default_native_surface_video_view

                apiImplResourceId = typedArray.getResourceId(R.styleable.VideoView_videoViewApiImpl, apiImplResourceId)
                apiImplLegacyResourceId = typedArray.getResourceId(R.styleable.VideoView_videoViewApiImplLegacy, apiImplLegacyResourceId)

            }
        }

        @SuppressLint("Recycle")
        private fun Context.withStyledAttributes(attributeSet: AttributeSet?, styles: IntArray, block: (TypedArray) -> Unit) {
            if (attributeSet == null) {
                return
            }

            obtainStyledAttributes(attributeSet, styles)?.let {
                block(it)
                it.recycle()
            }
        }
    }
}