/*
 * Copyright (C) 2016 Brian Wernick
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

package com.devbrackets.android.exomedia.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener;
import com.devbrackets.android.exomedia.util.EMDeviceUtil;
import com.devbrackets.android.exomedia.util.Repeater;
import com.devbrackets.android.exomedia.util.StopWatch;
import com.google.android.exoplayer.MediaFormat;

import java.util.List;
import java.util.Map;

/**
 * This is a support VideoView that will use the standard VideoView on devices below
 * JellyBean.  On devices with JellyBean and up we will use the ExoPlayer in order to
 * better support HLS streaming and full 1080p video resolutions which the VideoView
 * struggles with, and in some cases crashes.
 * <p>
 * To an external user this view should have the same APIs used with the standard VideoView
 * to help with quick implementations.
 */
@SuppressWarnings("UnusedDeclaration")
public class EMVideoView extends RelativeLayout {
    private static final String TAG = EMVideoView.class.getSimpleName();

    protected View shutterTop;
    protected View shutterBottom;
    protected View shutterRight;
    protected View shutterLeft;

    @Nullable
    protected VideoControls videoControls;
    protected ImageView previewImageView;

    protected Uri videoUri;
    protected VideoViewApi videoViewImpl;
    protected Repeater pollRepeater = new Repeater();

    protected int positionOffset = 0;
    protected int overriddenDuration = -1;

    protected boolean overridePosition = false;
    protected StopWatch overriddenPositionStopWatch = new StopWatch();

    protected MuxNotifier muxNotifier = new MuxNotifier();
    protected EMListenerMux listenerMux;

    protected boolean releaseOnDetachFromWindow = true;

    public EMVideoView(Context context) {
        super(context);
        setup(context, null);
    }

    public EMVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public EMVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EMVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (releaseOnDetachFromWindow) {
            release();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //Makes sure the shutters are the correct size
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateVideoShutters(getWidth(), getHeight(), videoViewImpl.getWidth(), videoViewImpl.getHeight());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        forceLayout();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            updateVideoShutters(right, bottom, videoViewImpl.getWidth(), videoViewImpl.getHeight());
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener listener) {
        videoViewImpl.setOnTouchListener(listener);

        //Sets the onTouch listener for the shutters
        shutterLeft.setOnTouchListener(listener);
        shutterRight.setOnTouchListener(listener);
        shutterTop.setOnTouchListener(listener);
        shutterBottom.setOnTouchListener(listener);

        super.setOnTouchListener(listener);
    }

    private void setup(Context context, @Nullable AttributeSet attrs) {
        initView(context, attrs);
        readAttributes(context, attrs);
    }

    /**
     * Reads the attributes associated with this view, setting any values found
     *
     * @param context The context to retrieve the styled attributes with
     * @param attrs The {@link AttributeSet} to retrieve the values from
     */
    private void readAttributes(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null || isInEditMode()) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EMVideoView);
        if (typedArray == null) {
            return;
        }

        //Updates the VideoControls if specified
        boolean useDefaultControls = typedArray.getBoolean(R.styleable.EMVideoView_useDefaultControls, false);
        if (useDefaultControls) {
            setControls(EMDeviceUtil.isDeviceTV(getContext()) ? new VideoControlsLeanback(getContext()) : new VideoControlsMobile(getContext()));
        }

        typedArray.recycle();
    }

    protected void initView(Context context, @Nullable AttributeSet attrs) {
        inflateVideoView(context, attrs);

        shutterBottom = findViewById(R.id.exomedia_video_shutter_bottom);
        shutterTop = findViewById(R.id.exomedia_video_shutter_top);
        shutterLeft = findViewById(R.id.exomedia_video_shutter_left);
        shutterRight = findViewById(R.id.exomedia_video_shutter_right);

        previewImageView = (ImageView) findViewById(R.id.exomedia_video_preview_image);
        videoViewImpl = (VideoViewApi) findViewById(R.id.exomedia_video_view);

        muxNotifier = new MuxNotifier();
        listenerMux = new EMListenerMux(muxNotifier);

        videoViewImpl.setListenerMux(listenerMux);
        videoViewImpl.setOnSizeChangedListener(muxNotifier);
    }

    /**
     * <b><em>WARNING:</em></b> Use of this method may cause memory leaks.
     * <p>
     * Enables or disables the automatic release when the EMVideoView is detached
     * from the window.  Normally this is expected to release all resources used
     * by calling {@link #release()}.  If <code>releaseOnDetach</code> is disabled
     * then {@link #release()} will need to be manually called.
     *
     * @param releaseOnDetach False to disable the automatic release in {@link #onDetachedFromWindow()}
     */
    public void setReleaseOnDetachFromWindow(boolean releaseOnDetach) {
        this.releaseOnDetachFromWindow = releaseOnDetach;
    }

    /**
     * Stops the playback and releases all resources attached to this
     * EMVideoView.  This should not be called manually unless
     * {@link #setReleaseOnDetachFromWindow(boolean)} has been set.
     */
    public void release() {
        videoControls = null;
        stopPlayback();
        overriddenPositionStopWatch.stop();

        videoViewImpl.release();
    }

    /**
     * Sets an image that will be visible only when the video is loading.
     *
     * @param drawable The drawable to use for the preview image
     */
    public void setPreviewImage(@Nullable Drawable drawable) {
        if (previewImageView != null) {
            previewImageView.setImageDrawable(drawable);
        }
    }

    /**
     * Sets an image that will be visible only when the video is loading.
     *
     * @param resourceId The resourceId representing the preview image
     */
    public void setPreviewImage(@DrawableRes int resourceId) {
        if (previewImageView != null) {
            previewImageView.setImageResource(resourceId);
        }
    }

    /**
     * Sets an image that will be visible only when the video is loading.
     *
     * @param bitmap The bitmap to use for the preview image
     */
    public void setPreviewImage(@Nullable Bitmap bitmap) {
        if (previewImageView != null) {
            previewImageView.setImageBitmap(bitmap);
        }
    }

    /**
     * Sets an image that will be visible only when the video is loading.
     *
     * @param uri The Uri pointing to the preview image
     */
    public void setPreviewImage(@Nullable Uri uri) {
        if (previewImageView != null) {
            previewImageView.setImageURI(uri);
        }
    }

    /**
     * Gets the preview ImageView for use with image loading libraries.
     *
     * @return the preview ImageView
     */
    public ImageView getPreviewImageView() {
        return previewImageView;
    }

    /**
     * Sets the color for the video shutters (the black bars above and below the video)
     *
     * @param color The color
     */
    public void setShutterColor(int color) {
        if (shutterTop != null) {
            shutterTop.setBackgroundColor(color);
        }

        if (shutterBottom != null) {
            shutterBottom.setBackgroundColor(color);
        }

        if (shutterLeft != null) {
            shutterLeft.setBackgroundColor(color);
        }

        if (shutterRight != null) {
            shutterRight.setBackgroundColor(color);
        }
    }

    public void setControls(@Nullable VideoControls controls) {
        if (videoControls != null && videoControls != controls) {
            removeView(videoControls);
        }

        if (controls != null) {
            videoControls = controls;
            controls.setVideoView(this);
            addView(controls);
        }

        //Sets the onTouch listener to show the controls
        TouchListener listener = new TouchListener(getContext());
        setOnTouchListener(videoControls != null ? listener : null);
    }

    /**
     * Requests the {@link VideoControls} to become visible.  This should only be called after
     * {@link #setControls(VideoControls)}.
     */
    public void showControls() {
        if (videoControls != null) {
            videoControls.show();

            if (isPlaying()) {
                videoControls.hideDelayed(VideoControls.DEFAULT_CONTROL_HIDE_DELAY);
            }
        }
    }

    /**
     * Retrieves the video controls being used by this view.
     * If the controls haven't been specified with {@link #setControls(VideoControls)}
     * or through the XML attribute <code>useDefaultControls</code> this will return
     * null
     *
     * @return The video controls being used by this view or null
     */
    @Nullable
    public VideoControls getVideoControls() {
        return videoControls;
    }

    /**
     * Sets the Uri location for the video to play
     *
     * @param uri The video's Uri
     */
    public void setVideoURI(@Nullable Uri uri) {
        videoUri = uri;
        videoViewImpl.setVideoUri(uri);

        if (videoControls != null) {
            videoControls.restartLoading();
        }
    }

    /**
     * Sets the Uri location for the video to play
     *
     * @param uri The video's Uri
     * @param renderBuilder RenderBuilder that should be used
     */
    public void setVideoURI(@Nullable Uri uri, @Nullable RenderBuilder renderBuilder) {
        videoUri = uri;
        videoViewImpl.setVideoUri(uri, renderBuilder);

        if (videoControls != null) {
            videoControls.restartLoading();
        }
    }

    /**
     * Sets the path to the video.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param path The path to the video
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Retrieves the current Video URI.  If this hasn't been set with {@link #setVideoURI(android.net.Uri)}
     * or {@link #setVideoPath(String)} then null will be returned.
     *
     * @return The current video URI or null
     */
    @Nullable
    public Uri getVideoUri() {
        return videoUri;
    }

    /**
     * Sets the volume level for devices that support
     * the ExoPlayer (JellyBean or greater).
     *
     * @param volume The volume range [0.0 - 1.0]
     * @return True if the volume was set
     */
    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        return videoViewImpl.setVolume(volume);
    }

    /**
     * Stops the current video playback and resets the listener states
     * so that we receive the callbacks for events like onPrepared
     */
    public void reset() {
        stopPlayback();
        setVideoURI(null);
    }

    /**
     * Moves the current video progress to the specified location.
     *
     * @param milliSeconds The time to move the playback to
     */
    public void seekTo(int milliSeconds) {
        videoViewImpl.seekTo(milliSeconds);
    }

    /**
     * Returns if a video is currently in playback
     *
     * @return True if a video is playing
     */
    public boolean isPlaying() {
        return videoViewImpl.isPlaying();
    }

    /**
     * Starts the playback for the video specified in {@link #setVideoURI(android.net.Uri)}
     * or {@link #setVideoPath(String)}.  This should be called after the VideoView is correctly
     * prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     */
    public void start() {
        videoViewImpl.start();
        setKeepScreenOn(true);

        if (videoControls != null) {
            videoControls.updatePlaybackState(true);
        }
    }

    /**
     * If a video is currently in playback, it will be paused
     */
    public void pause() {
        videoViewImpl.pause();
        setKeepScreenOn(false);

        if (videoControls != null) {
            videoControls.updatePlaybackState(false);
        }
    }

    /**
     * If a video is currently in playback then the playback will be stopped
     */
    public void stopPlayback() {
        videoViewImpl.stopPlayback();
        setKeepScreenOn(false);

        if (videoControls != null) {
            videoControls.updatePlaybackState(false);
        }
    }

    /**
     * If the video has completed playback, calling {@code restart} will seek to the beginning of the video, and play it.
     *
     * @return {@code true} if the video was successfully restarted, otherwise {@code false}
     */
    public boolean restart() {
        if (videoUri == null) {
            return false;
        }

        if (videoViewImpl.restart()) {
            if (videoControls != null) {
                videoControls.restartLoading();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * If a video is currently in playback then the playback will be suspended
     */
    public void suspend() {
        videoViewImpl.suspend();
        setKeepScreenOn(false);

        if (videoControls != null) {
            videoControls.updatePlaybackState(false);
        }
    }

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared (see {@link #setOnPreparedListener(OnPreparedListener)}).
     * If {@link #overrideDuration(int)} is set then that value will be returned.
     *
     * @return The millisecond duration of the video
     */
    public int getDuration() {
        if (overriddenDuration >= 0) {
            return overriddenDuration;
        }

        return videoViewImpl.getDuration();
    }

    /**
     * Setting this will override the duration that the item may actually be.  This method should
     * only be used when the item doesn't return the correct duration such as with audio streams.
     * This only overrides the current audio item.
     *
     * @param duration The duration for the current media item or &lt; 0 to disable
     */
    public void overrideDuration(int duration) {
        overriddenDuration = duration;
    }

    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     *
     * @return The millisecond value for the current position
     */
    public int getCurrentPosition() {
        if (overridePosition) {
            return positionOffset + overriddenPositionStopWatch.getTimeInt();
        }

        return positionOffset + videoViewImpl.getCurrentPosition();
    }

    /**
     * Sets the amount of time to change the return value from {@link #getCurrentPosition()}.
     * This value will be reset when a new audio item is selected.
     *
     * @param offset The millisecond value to offset the position
     */
    public void setPositionOffset(int offset) {
        positionOffset = offset;
    }

    /**
     * Restarts the audio position to the start if the position is being overridden (see {@link #overridePosition(boolean)}).
     * This will be the value specified with {@link #setPositionOffset(int)} or 0 if it hasn't been set.
     */
    public void restartOverridePosition() {
        overriddenPositionStopWatch.reset();
    }

    /**
     * Sets if the audio position should be overridden, allowing the time to be restarted at will.  This
     * is useful for streaming audio where the audio doesn't have breaks between songs.
     *
     * @param override True if the position should be overridden
     */
    public void overridePosition(boolean override) {
        if (override) {
            overriddenPositionStopWatch.start();
        } else {
            overriddenPositionStopWatch.stop();
        }

        overridePosition = override;
    }

    /**
     * Retrieves the current buffer percent of the video.  If a video is not currently
     * prepared or buffering the value will be 0.  This should only be called after the video is
     * prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    public int getBufferPercentage() {
        return videoViewImpl.getBufferedPercent();
    }

    /**
     * Determines if the current video player implementation supports
     * track selection for audio or video tracks.
     *
     * @return True if tracks can be manually specified
     */
    public boolean trackSelectionAvailable() {
        return videoViewImpl.trackSelectionAvailable();
    }

    /**
     * Changes to the track with <code>trackIndex</code> for the specified
     * <code>trackType</code>
     *
     * @param trackType The type for the track to switch to the selected index
     * @param trackIndex The index for the track to swith to
     */
    public void setTrack(@TrackRenderType int trackType, int trackIndex) {
        videoViewImpl.setTrack(trackType, trackIndex);
    }

    /**
     * Retrieves a list of available tracks to select from.  Typically {@link #trackSelectionAvailable()}
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type (see {@link com.devbrackets.android.exomedia.annotation.TrackRenderType})
     */
    @Nullable
    public Map<Integer, List<MediaFormat>> getAvailableTracks() {
        return videoViewImpl.getAvailableTracks();
    }

    /**
     * Sets how the video should be scaled in the view
     *
     * @param scaleType how to scale the videos
     */
    public void setScaleType(@NonNull ScaleType scaleType) {
        videoViewImpl.setScaleType(scaleType);

        int width = getWidth();
        int height = getHeight();
        if(width > 0 && height > 0) {
            updateVideoShutters(width, height, videoViewImpl.getWidth(), videoViewImpl.getHeight());
        }
    }

  /**
   * Measures the underlying {@link VideoViewApi} using the video's aspect ratio if {@code true}
   *
   * @param measureBasedOnAspectRatioEnabled whether to measure using the video's aspect ratio or not
   */
  public void setMeasureBasedOnAspectRatioEnabled(boolean measureBasedOnAspectRatioEnabled) {
        videoViewImpl.setMeasureBasedOnAspectRatioEnabled(measureBasedOnAspectRatioEnabled);
    }

    /**
     * Sets the rotation for the Video
     *
     * @param rotation The rotation to apply to the video
     */
    public void setVideoRotation(@IntRange(from = 0, to = 359) int rotation) {
        videoViewImpl.setVideoRotation(rotation, true);
    }

    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        listenerMux.setOnPreparedListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        listenerMux.setOnCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer buffer update events
     *
     * @param listener The listener
     */
    public void setOnBufferUpdateListener(OnBufferUpdateListener listener) {
        listenerMux.setOnBufferUpdateListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer seek completion events
     *
     * @param listener The listener
     */
    public void setOnSeekCompletionListener(OnSeekCompletionListener listener) {
        listenerMux.setOnSeekCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener
     */
    public void setOnErrorListener(OnErrorListener listener) {
        listenerMux.setOnErrorListener(listener);
    }

    /**
     * Inflates the video view layout, replacing the {@link ViewStub} with the
     * correct backing implementation.
     *
     * @param context The context to use for inflating the correct video view
     * @param attrs The attributes for retrieving custom backing implementations.
     */
    protected void inflateVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        View.inflate(context, R.layout.exomedia_video_view_layout, this);
        ViewStub videoViewStub = (ViewStub) findViewById(R.id.video_view_api_impl_stub);

        videoViewStub.setLayoutResource(getVideoViewApiImplementation(context, attrs));
        videoViewStub.inflate();
    }

    /**
     * Retrieves the layout resource to use for the backing video view implementation.  By
     * default this uses the Android {@link android.widget.VideoView} on legacy devices with
     * APIs below Jellybean (16) or that don't pass the Compatibility Test Suite [CTS] via
     * {@link com.devbrackets.android.exomedia.core.video.NativeVideoView}
     * , and an ExoPlayer backed video view on the remaining devices via
     * {@link com.devbrackets.android.exomedia.core.video.ExoVideoView}.
     * <p>
     * In the rare cases that the default implementations need to be extended, or replaced, the
     * user can override the value with the attributes <code>videoViewApiImplLegacy</code>
     * and <code>videoViewApiImpl</code>.
     * <p>
     * <b>NOTE:</b> overriding the default implementations may cause inconsistencies and isn't
     * recommended.
     *
     * @param context The Context to use when retrieving the backing video view implementation
     * @param attrs The attributes to use for finding overridden video view implementations
     * @return The layout resource for the backing implementation on the current device
     */
    @LayoutRes
    protected int getVideoViewApiImplementation(@NonNull Context context, @Nullable AttributeSet attrs) {
        boolean useLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || !EMDeviceUtil.isDeviceCTSCompliant();
        int defaultVideoViewApiImplRes = useLegacy ? R.layout.exomedia_default_native_video_view : R.layout.exomedia_default_exo_video_view;

        if (attrs == null || isInEditMode()) {
            return defaultVideoViewApiImplRes;
        }

        //If there aren't any EMVideoView styles specified, return the default implementation
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EMVideoView);
        if (typedArray == null) {
            return defaultVideoViewApiImplRes;
        }

        //Retrieves the specified implementation
        int styleableRes = useLegacy ? R.styleable.EMVideoView_videoViewApiImplLegacy : R.styleable.EMVideoView_videoViewApiImpl;
        int videoViewApiImplRes = typedArray.getResourceId(styleableRes, defaultVideoViewApiImplRes);
        typedArray.recycle();

        return videoViewApiImplRes;
    }

    /**
     * Performs the functionality to stop the progress polling, and stop any other
     * procedures from running that we no longer need.
     */
    protected void onPlaybackEnded() {
        stopPlayback();
        pollRepeater.stop();
    }

    protected void updateVideoShutters(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        //Sets the horizontal shutter (top and bottom) sizes
        int shutterHeight = calculateVerticalShutterSize(viewHeight, videoHeight);
        if (shutterTop != null) {
            shutterTop.getLayoutParams().height = shutterHeight;
            shutterTop.requestLayout();
        }

        if (shutterBottom != null) {
            shutterBottom.getLayoutParams().height = shutterHeight;
            shutterBottom.requestLayout();
        }

        //Sets the vertical shutter (left and right) sizes
        int shutterWidth = calculateSideShutterSize(viewWidth, videoWidth);
        if (shutterLeft != null) {
            shutterLeft.getLayoutParams().width = shutterWidth;
            shutterLeft.requestLayout();
        }

        if (shutterRight != null) {
            shutterRight.getLayoutParams().width = shutterWidth;
            shutterRight.requestLayout();
        }
    }

    protected int calculateVerticalShutterSize(int viewHeight, int videoHeight) {
        if(videoViewImpl.getScaleType() == ScaleType.CENTER_CROP) {
            return 0;
        }

        int shutterSize = (viewHeight - videoHeight) / 2;
        return (viewHeight - videoHeight) % 2 == 0 ? shutterSize : shutterSize + 1;
    }

    protected int calculateSideShutterSize(int viewWidth, int videoWidth) {
        if(videoViewImpl.getScaleType() == ScaleType.CENTER_CROP) {
            return 0;
        }

        int shutterSize = (viewWidth - videoWidth) / 2;
        return (viewWidth - videoWidth) % 2 == 0 ? shutterSize : shutterSize + 1;
    }

    protected class MuxNotifier extends EMListenerMux.EMListenerMuxNotifier implements VideoViewApi.OnSurfaceSizeChanged {
        public static final int ROTATION_90 = 90;
        public static final int ROTATION_270 = 270;

        @Override
        public boolean shouldNotifyCompletion(long endLeeway) {
            return getCurrentPosition() + endLeeway >= getDuration();
        }

        @Override
        public void onExoPlayerError(EMExoPlayer emExoPlayer, Exception e) {
            stopPlayback();

            if (emExoPlayer != null) {
                emExoPlayer.forcePrepare();
            }
        }

        @Override
        public void onMediaPlaybackEnded() {
            setKeepScreenOn(false);
            onPlaybackEnded();
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {
            updateVideoShutters(getWidth(), getHeight(), width, height);
        }

        @Override
        @SuppressWarnings("SuspiciousNameCombination")
        public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees, float pixelWidthHeightRatio) {
            //NOTE: Android 5.0+ will always have an unAppliedRotationDegrees of 0 (ExoPlayer already handles it)
            videoViewImpl.setVideoRotation(unAppliedRotationDegrees, false);

            //If we applied a rotation make sure to update the width, height, and ratio
            if (unAppliedRotationDegrees == ROTATION_90 || unAppliedRotationDegrees == ROTATION_270) {
                int rotatedHeight = height;
                height = width;
                width = rotatedHeight;
                pixelWidthHeightRatio = 1 / pixelWidthHeightRatio;
            }

            onVideoSizeChanged(width, height, pixelWidthHeightRatio);
        }

        @Override
        public void onPrepared() {
            if (videoControls != null) {
                videoControls.setDuration(getDuration());
                videoControls.loadCompleted();
            }
        }

        @Override
        public void onPreviewImageStateChanged(boolean toVisible) {
            if (previewImageView != null) {
                previewImageView.setVisibility(toVisible ? View.VISIBLE : View.GONE);
            }
        }

        public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
            videoViewImpl.onVideoSizeChanged(width, height);

            //Since the ExoPlayer will occasionally return an unscaled video size, we will make sure
            // we are using scaled values when updating the shutters
            if (width < getWidth() && height < getHeight()) {
                //Makes sure we have the correct aspect ratio
                float videoAspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;

                width = getWidth();
                height = (int) (width / videoAspectRatio);
                if (height > getHeight()) {
                    height = getHeight();
                    width = (int) (height * videoAspectRatio);
                }
            }

            updateVideoShutters(getWidth(), getHeight(), width, height);
        }
    }

    /**
     * Monitors the view click events to show the video controls if they have been specified.
     */
    protected class TouchListener extends GestureDetector.SimpleOnGestureListener implements OnTouchListener {
        protected GestureDetector gestureDetector;

        public TouchListener(Context context) {
            gestureDetector = new GestureDetector(context, this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (videoControls != null) {
                videoControls.show();

                if (isPlaying()) {
                    videoControls.hideDelayed(VideoControls.DEFAULT_CONTROL_HIDE_DELAY);
                }
            }

            return true;
        }
    }
}