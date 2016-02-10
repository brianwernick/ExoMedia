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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.exomedia.util.EMDeviceUtil;
import com.devbrackets.android.exomedia.util.Repeater;
import com.devbrackets.android.exomedia.util.StopWatch;

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
    private EMListenerMux listenerMux;

    protected boolean releaseOnDetachFromWindow = true;

    public EMVideoView(Context context) {
        super(context);
        setup(null);
    }

    public EMVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public EMVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EMVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();

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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            updateVideoShutters(r, b, videoViewImpl.getWidth(), videoViewImpl.getHeight());
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener listener) {
        videoViewImpl.setOnTouchListener(listener);

        //Sets the onTouch listener for the shutters
        if(shutterLeft != null) {
            shutterLeft.setOnTouchListener(listener);
        }

        if(shutterRight != null) {
            shutterRight.setOnTouchListener(listener);
        }

        if(shutterTop != null) {
            shutterTop.setOnTouchListener(listener);
        }

        if(shutterBottom != null) {
            shutterBottom.setOnTouchListener(listener);
        }

        super.setOnTouchListener(listener);
    }

    private void setup(@Nullable AttributeSet attrs) {
        //TODO: we need this in the DefaultControls
//        pollRepeater.setRepeatListener(new Repeater.RepeatListener() {
//            @Override
//            public void onRepeat() {
//                currentMediaProgressEvent.update(getCurrentPosition(), getBufferPercentage(), getDuration());
//
//                if (defaultControls != null) {
//                    defaultControls.setProgressEvent(currentMediaProgressEvent);
//                }
//            }
//        });

        TypedArray typedArray = null;
        if (attrs != null && !isInEditMode()) {
            typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.EMVideoView);
        }

        initView(typedArray);
        if(typedArray != null) {
            initDefaultControls(typedArray);
            typedArray.recycle();
        }
    }

    private void initDefaultControls(@NonNull TypedArray typedArray) {
        //Updates the VideoControls if specified
        boolean useDefaultControls = typedArray.getBoolean(R.styleable.EMVideoView_useDefaultControls, false);
        if (useDefaultControls) {
            setControls(EMDeviceUtil.isDeviceTV(getContext()) ? new VideoControlsLeanback(getContext()) : new VideoControlsMobile(getContext()));
        }
    }

    private void initView(@Nullable TypedArray typedArray) {
        View.inflate(getContext(), R.layout.exomedia_video_view_layout, this);

        previewImageView = (ImageView) findViewById(R.id.exomedia_video_preview_image);

        inflateVideoViewImpl(typedArray);

        videoViewImpl = (VideoViewApi) findViewById(R.id.exomedia_video_view);

        initShutterViews(typedArray);

        muxNotifier = new MuxNotifier();
        listenerMux = new EMListenerMux(muxNotifier);

        videoViewImpl.setListenerMux(listenerMux);
        videoViewImpl.setOnSizeChangedListener(muxNotifier);
    }

    private void initShutterViews(@Nullable TypedArray typedArray) {
        shutterBottom = findViewById(R.id.exomedia_video_shutter_bottom);
        shutterTop = findViewById(R.id.exomedia_video_shutter_top);
        shutterLeft = findViewById(R.id.exomedia_video_shutter_left);
        shutterRight = findViewById(R.id.exomedia_video_shutter_right);

        if(typedArray != null) {
            boolean hideShutters = typedArray.getBoolean(R.styleable.EMVideoView_videoViewHideShutters, false);

            if(hideShutters) {
                ViewGroup.LayoutParams lp = ((View) videoViewImpl).getLayoutParams();
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;

                removeView(shutterBottom);
                removeView(shutterTop);
                removeView(shutterLeft);
                removeView(shutterRight);

                shutterBottom = null;
                shutterTop = null;
                shutterLeft = null;
                shutterRight = null;
            }
        }
    }

    private void inflateVideoViewImpl(@Nullable TypedArray typedArray) {
        final boolean useLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || !EMDeviceUtil.isDeviceCTSCompliant();

        ViewStub videoViewImplStub = (ViewStub) findViewById(R.id.video_view_api_impl_stub);

        final @LayoutRes int defaultVideoViewApiImplRes;
        if(useLegacy) {
            defaultVideoViewApiImplRes = R.layout.exomedia_default_native_video_view;
        }
        else {
            defaultVideoViewApiImplRes = R.layout.exomedia_default_exo_video_view;
        }

        final @LayoutRes int videoViewApiImplRes;
        if (typedArray == null) {
            videoViewApiImplRes = defaultVideoViewApiImplRes;
        } else {
            if(useLegacy) {
                videoViewApiImplRes = typedArray.getResourceId(R.styleable.EMVideoView_videoViewApiImplLegacy, defaultVideoViewApiImplRes);
            } else {
                videoViewApiImplRes = typedArray.getResourceId(R.styleable.EMVideoView_videoViewApiImpl, defaultVideoViewApiImplRes);
            }
        }

        videoViewImplStub.setLayoutResource(videoViewApiImplRes);
        videoViewImplStub.inflate();
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
     * @param renderBuilder    RenderBuilder that should be used
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
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     */
    public void start() {
        videoViewImpl.start();

        if (videoControls != null) {
            videoControls.updatePlayPauseImage(true);
            videoControls.hideDelayed(VideoControls.DEFAULT_CONTROL_HIDE_DELAY);
        }
    }

    /**
     * If a video is currently in playback, it will be paused
     */
    public void pause() {
        videoViewImpl.pause();

        if (videoControls != null) {
            videoControls.updatePlayPauseImage(false);
            videoControls.show();
        }
    }

    /**
     * If a video is currently in playback then the playback will be stopped
     */
    public void stopPlayback() {
        videoViewImpl.stopPlayback();

        if (videoControls != null) {
            videoControls.updatePlayPauseImage(false);
            videoControls.show();
        }
    }

    /**
     * If a video is currently in playback then the playback will be suspended
     */
    public void suspend() {
        videoViewImpl.suspend();

        if (videoControls != null) {
            videoControls.updatePlayPauseImage(false);
            videoControls.show();
        }
    }

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)}).
     * If {@link #overrideDuration(int)} is set then that value will be returned.
     *
     * @return The millisecond duration of the video
     */
    public long getDuration() {
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
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     *
     * @return The millisecond value for the current position
     */
    public long getCurrentPosition() {
        if (overridePosition) {
            return positionOffset + overriddenPositionStopWatch.getTime();
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
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    public int getBufferPercentage() {
        return videoViewImpl.getBufferedPercent();
    }

    /**
     * Sets the listener to inform of any exoPlayer events
     *
     * @param listener The listener
     */
    public void addExoPlayerListener(ExoPlayerListener listener) {
        listenerMux.addExoPlayerListener(listener);
    }

    /**
     * Removes the specified listener for the ExoPlayer.
     *
     * @param listener The listener to remove
     */
    public void removeExoPlayerListener(ExoPlayerListener listener) {
        listenerMux.removeExoPlayerListener(listener);
    }


    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        listenerMux.setOnPreparedListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        listenerMux.setOnCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener
     */
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        listenerMux.setOnErrorListener(listener);
    }

    /**
     * Sets the listener to inform of media information events.
     *
     * @param listener The listener
     */
    public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
        listenerMux.setOnInfoListener(listener);
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
        int shutterSize = (viewHeight - videoHeight) / 2;
        return (viewHeight - videoHeight) % 2 == 0 ? shutterSize : shutterSize +1;
    }

    protected int calculateSideShutterSize(int viewWidth, int videoWidth) {
        int shutterSize = (viewWidth - videoWidth) / 2;
        return (viewWidth - videoWidth) % 2 == 0 ? shutterSize : shutterSize +1;
    }

    protected class MuxNotifier extends EMListenerMux.EMListenerMuxNotifier implements VideoViewApi.OnSurfaceSizeChanged {
        @Override
        public boolean shouldNotifyCompletion(long endLeeway) {
            return getCurrentPosition() + endLeeway >= getDuration();
        }

        @Override
        public void onExoPlayerError(EMExoPlayer emExoPlayer, Exception e) {
            if (emExoPlayer != null) {
                emExoPlayer.forcePrepare();
            }
        }

        @Override
        public void onMediaPlaybackEnded() {
            onPlaybackEnded();
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {
            updateVideoShutters(getWidth(), getHeight(), width, height);
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees, float pixelWidthHeightRatio) {
            //Makes sure we have the correct aspect ratio
            float videoAspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
            videoViewImpl.updateAspectRatio(videoAspectRatio);

            //Since the ExoPlayer will occasionally return an unscaled video size, we will make sure
            // we are using scaled values when updating the shutters
            if (width < getWidth() && height < getHeight()) {
                width = getWidth();
                height = (int)(width / videoAspectRatio);
                if (height > getHeight()) {
                    height = getHeight();
                    width = (int)(height * videoAspectRatio);
                }
            }

            updateVideoShutters(getWidth(), getHeight(), width, height);
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
    }

    /**
     * Monitors the view click events to show the video controls if they have been specified.
     */
    private class TouchListener extends GestureDetector.SimpleOnGestureListener implements OnTouchListener {
        private GestureDetector gestureDetector;

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