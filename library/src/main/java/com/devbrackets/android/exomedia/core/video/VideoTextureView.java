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
import android.graphics.Point;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * A VideoTextureView that reSizes itself to match a specified aspect
 * ratio for videos.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoTextureView extends TextureView {
    /**
     * The surface view will not resize itself if the fractional difference between its default
     * aspect ratio and the aspect ratio of the video falls below this threshold.
     * <p>
     * This tolerance is useful for fullscreen playbacks, since it ensures that the surface will
     * occupy the whole of the screen when playing content that has the same (or virtually the same)
     * aspect ratio as the device. This typically reduces the number of view layers that need to be
     * composited by the underlying system, which can help to reduce power consumption.
     */
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION  = 0.01f;

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

    protected float videoAspectRatio;

    @Nullable
    protected OnSizeChangeListener listener;
    protected Point oldSize = new Point(0, 0);

    @Nullable
    Surface surface;

    public VideoTextureView(Context context) {
        super(context);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Set the aspect ratio that this {@link VideoTextureView} should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio == 0) {
            // Aspect ratio not set.
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance, so leave the values from super
            notifyListener(width, height);
            return;
        }

        if (aspectDeformation > 0) {
            height = (int) (width / videoAspectRatio);
        } else {
            width = (int) (height * videoAspectRatio);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        notifyListener(width, height);
    }

    public void setOnSizeChangeListener(@Nullable OnSizeChangeListener listener) {
        this.listener = listener;
    }

    public void clearSurface() {
        if (surface == null) {
            return;
        }

        EGL10 gl10 = (EGL10) EGLContext.getEGL();
        EGLDisplay display = gl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        gl10.eglInitialize(display, null);

        EGLConfig[] configs = new EGLConfig[1];
        gl10.eglChooseConfig(display, GL_CLEAR_CONFIG_ATTRIBUTES, configs, configs.length, new int[1]);
        EGLContext context = gl10.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, GL_CLEAR_CONTEXT_ATTRIBUTES);
        EGLSurface eglSurface = gl10.eglCreateWindowSurface(display, configs[0], surface, new int[] { EGL10.EGL_NONE });

        gl10.eglMakeCurrent(display, eglSurface, eglSurface, context);

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        gl10.eglSwapBuffers(display, eglSurface);
        gl10.eglDestroySurface(display, eglSurface);
        gl10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        gl10.eglDestroyContext(display, context);

        gl10.eglTerminate(display);
    }

    protected void notifyListener(int width, int height) {
        if (listener != null && (oldSize.x != width || oldSize.y != height)) {
            oldSize.x = width;
            oldSize.y = height;
            listener.onVideoSurfaceSizeChange(width, height);
        }
    }
}
