/*
 * Copyright (C) 2016 Brian Wernick,
 * Copyright (C) 2014 The Android Open Source Project
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

package com.devbrackets.android.exomedia.core.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;

import com.devbrackets.android.exomedia.core.video.scale.MatrixManager;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;

import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * A SurfaceView that reSizes itself according to the requested layout type
 * once we have a video
 */
public class ResizingSurfaceView extends SurfaceView implements ClearableSurface {
    private static final String TAG = "ResizingSurfaceView";
    protected static final int MAX_DEGREES = 360;

    /**
     * A version of the EGL14.EGL_CONTEXT_CLIENT_VERSION so that we can
     * reference it without being on API 17+
     */
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform teh clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * the configuration of that context
     */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final int[] GL_CLEAR_CONFIG_ATTRIBUTES = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_NONE, 0,
            EGL10.EGL_NONE
    };

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform teh clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * that context
     */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final int[] GL_CLEAR_CONTEXT_ATTRIBUTES = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
    };

    public interface OnSizeChangeListener {
        void onVideoSurfaceSizeChange(int width, int height);
    }

    @Nullable
    protected OnSizeChangeListener onSizeChangeListener;

    @NonNull
    protected Point lastNotifiedSize = new Point(0, 0);
    @NonNull
    protected Point videoSize = new Point(0, 0);

    @NonNull
    protected MatrixManager matrixManager = new MatrixManager();

    @NonNull
    protected AttachedListener attachedListener = new AttachedListener();
    @NonNull
    protected GlobalLayoutMatrixListener globalLayoutMatrixListener = new GlobalLayoutMatrixListener();
    @NonNull
    protected final ReentrantLock globalLayoutMatrixListenerLock = new ReentrantLock(true);

    @IntRange(from = 0, to = 359)
    protected int requestedUserRotation = 0;
    @IntRange(from = 0, to = 359)
    protected int requestedConfigurationRotation = 0;

    // This is purposefully true to support older devices (below API 21) which
    // isn't needed by the ResizingTextureView (which is false be default)
    protected boolean measureBasedOnAspectRatio = true;

    public ResizingSurfaceView(Context context) {
        super(context);
    }

    public ResizingSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ResizingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!measureBasedOnAspectRatio) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            notifyOnSizeChangeListener(getMeasuredWidth(), getMeasuredHeight());
            return;
        }

        int width = getDefaultSize(videoSize.x, widthMeasureSpec);
        int height = getDefaultSize(videoSize.y, heightMeasureSpec);

        if (videoSize.x <= 0 || videoSize.y <= 0) {
            setMeasuredDimension(width, height);
            notifyOnSizeChangeListener(width, height);
            return;
        }

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
            width = widthSpecSize;
            height = heightSpecSize;

            // for compatibility, we adjust size based on aspect ratio
            if (videoSize.x * height < width * videoSize.y) {
                width = height * videoSize.x / videoSize.y;
            } else if (videoSize.x * height > width * videoSize.y) {
                height = width * videoSize.y / videoSize.x;
            }
        } else if (widthSpecMode == MeasureSpec.EXACTLY) {
            // only the width is fixed, adjust the height to match aspect ratio if possible
            width = widthSpecSize;
            height = width * videoSize.y / videoSize.x;
            if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                // couldn't match aspect ratio within the constraints
                height = heightSpecSize;
            }
        } else if (heightSpecMode == MeasureSpec.EXACTLY) {
            // only the height is fixed, adjust the width to match aspect ratio if possible
            height = heightSpecSize;
            width = height * videoSize.x / videoSize.y;
            if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                // couldn't match aspect ratio within the constraints
                width = widthSpecSize;
            }
        } else {
            // neither the width nor the height are fixed, try to use actual video size
            width = videoSize.x;
            height = videoSize.y;
            if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                // too tall, decrease both width and height
                height = heightSpecSize;
                width = height * videoSize.x / videoSize.y;
            }
            if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                // too wide, decrease both width and height
                width = widthSpecSize;
                height = width * videoSize.y / videoSize.x;
            }
        }

        setMeasuredDimension(width, height);
        notifyOnSizeChangeListener(width, height);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        updateMatrixOnLayout();
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Specifies the listener to notify of surface size changes.
     *
     * @param listener The listener to notify of surface size changes
     */
    public void setOnSizeChangeListener(@Nullable OnSizeChangeListener listener) {
        this.onSizeChangeListener = listener;
    }

    /**
     * Clears the frames from the current surface.  This should only be called when
     * the implementing video view has finished playback or otherwise released
     * the surface
     */
    @Override
    public void clearSurface() {
        try {
            EGL10 gl10 = (EGL10) EGLContext.getEGL();
            EGLDisplay display = gl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            gl10.eglInitialize(display, null);

            EGLConfig[] configs = new EGLConfig[1];
            gl10.eglChooseConfig(display, GL_CLEAR_CONFIG_ATTRIBUTES, configs, configs.length, new int[1]);
            EGLContext context = gl10.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, GL_CLEAR_CONTEXT_ATTRIBUTES);
            EGLSurface eglSurface = gl10.eglCreateWindowSurface(display, configs[0], this, new int[]{EGL10.EGL_NONE});

            gl10.eglMakeCurrent(display, eglSurface, eglSurface, context);
            gl10.eglSwapBuffers(display, eglSurface);
            gl10.eglDestroySurface(display, eglSurface);
            gl10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            gl10.eglDestroyContext(display, context);

            gl10.eglTerminate(display);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing surface", e);
        }
    }

    /**
     * Updates the stored videoSize and updates the default buffer size
     * in the backing texture view.
     *
     * @param width The width for the video
     * @param height The height for the video
     * @return True if the surfaces DefaultBufferSize was updated
     */
    protected boolean updateVideoSize(int width, int height) {
        matrixManager.setIntrinsicVideoSize(width, height);
        updateMatrixOnLayout();

        videoSize.x = width;
        videoSize.y = height;

        return width != 0 && height != 0;
    }

    /**
     * Sets the scaling method to use for the video
     *
     * @param scaleType The scale type to use
     */
    public void setScaleType(@NonNull ScaleType scaleType) {
        matrixManager.scale(this, scaleType);
    }

    /**
     * Retrieves the current {@link ScaleType} being used
     *
     * @return The current {@link ScaleType} being used
     */
    @NonNull
    public ScaleType getScaleType() {
        return matrixManager.getCurrentScaleType();
    }

    /**
     * Specifies if the {@link #onMeasure(int, int)} should pay attention to the specified
     * aspect ratio for the video (determined from {@link #videoSize}.
     *
     * @param enabled True if {@link #onMeasure(int, int)} should pay attention to the videos aspect ratio
     */
    public void setMeasureBasedOnAspectRatioEnabled(boolean enabled) {
        this.measureBasedOnAspectRatio = enabled;
        requestLayout();
    }

    /**
     * Sets the rotation for the Video
     *
     * @param rotation The rotation to apply to the video
     * @param fromUser True if the rotation was requested by the user, false if it is from a video configuration
     */
    public void setVideoRotation(@IntRange(from = 0, to = 359) int rotation, boolean fromUser) {
        setVideoRotation(fromUser ? rotation : requestedUserRotation, !fromUser ? rotation : requestedConfigurationRotation);
    }

    /**
     * Specifies the rotation that should be applied to the video for both the user
     * requested value and the value specified in the videos configuration.
     *
     * @param userRotation The rotation the user wants to apply
     * @param configurationRotation The rotation specified in the configuration for the video
     */
    public void setVideoRotation(@IntRange(from = 0, to = 359) int userRotation, @IntRange(from = 0, to = 359) int configurationRotation) {
        requestedUserRotation = userRotation;
        requestedConfigurationRotation = configurationRotation;

        matrixManager.rotate(this, (userRotation + configurationRotation) % MAX_DEGREES);
    }

    /**
     * Requests for the Matrix to be updated on layout changes.  This will
     * ensure that the scaling is correct and the rotation is not lost or
     * applied incorrectly.
     */
    protected void updateMatrixOnLayout() {
        globalLayoutMatrixListenerLock.lock();

        // if we're not attached defer adding the layout listener until we are
        if (getWindowToken() == null) {
            addOnAttachStateChangeListener(attachedListener);
        } else {
            getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutMatrixListener);
        }

        globalLayoutMatrixListenerLock.unlock();
    }

    /**
     * Performs the functionality to notify the listener that the
     * size of the surface has changed filtering out duplicate calls.
     *
     * @param width The new width
     * @param height The new height
     */
    protected void notifyOnSizeChangeListener(int width, int height) {
        if (lastNotifiedSize.x == width && lastNotifiedSize.y == height) {
            return;
        }

        lastNotifiedSize.x = width;
        lastNotifiedSize.y = height;

        updateMatrixOnLayout();

        if (onSizeChangeListener != null) {
            onSizeChangeListener.onVideoSurfaceSizeChange(width, height);
        }
    }

    /**
     * This is separated from the {@link ResizingSurfaceView#onAttachedToWindow()}
     * so that we have control over when it is added and removed
     */
    private class AttachedListener implements OnAttachStateChangeListener {
        @Override
        public void onViewAttachedToWindow(View view) {
            globalLayoutMatrixListenerLock.lock();

            getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutMatrixListener);
            removeOnAttachStateChangeListener(this);

            globalLayoutMatrixListenerLock.unlock();
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            //Purposefully left blank
        }
    }

    /**
     * Listens to the global layout to reapply the scale
     */
    private class GlobalLayoutMatrixListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            // Updates the scale to make sure one is applied
            setScaleType(matrixManager.getCurrentScaleType());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                //noinspection deprecation
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        }
    }
}