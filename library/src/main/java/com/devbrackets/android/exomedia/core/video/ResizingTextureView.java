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
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;

/**
 * A TextureView that reSizes itself according to the requested layout type
 * once we have a video
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ResizingTextureView extends TextureView {
    protected static final int MAX_DEGREES = 360;

    private boolean addGlobalLayoutListenerRequested;
    private final Object globalLayoutListenerLock = new Object();

    private final OnAttachStateChangeListener attachStateChangeListener = new OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
            removeOnAttachStateChangeListener(this);

            synchronized (globalLayoutListenerLock) {
                if(addGlobalLayoutListenerRequested) {
                    addGlobalLayoutListenerRequested = false;
                    getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
                }
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {}
    };

    private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            setScaleType(currentScaleType);
            setVideoRotation(requestedUserRotation, requestedConfigurationRotation);
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
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
    protected ScaleType currentScaleType = ScaleType.CENTER_INSIDE;
    @NonNull
    protected MatrixManager matrixManager = new MatrixManager();

    @IntRange(from = 0, to = 359)
    protected int requestedUserRotation = 0;
    @IntRange(from = 0, to = 359)
    protected int requestedConfigurationRotation = 0;

    public ResizingTextureView(Context context) {
        super(context);
    }

    public ResizingTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizingTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ResizingTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

    public void setOnSizeChangeListener(@Nullable OnSizeChangeListener listener) {
        this.onSizeChangeListener = listener;
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

        if (width == 0 || height == 0) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            getSurfaceTexture().setDefaultBufferSize(width, height);
        }

        return true;
    }

    public void setScaleType(@NonNull ScaleType scaleType) {
        currentScaleType = scaleType;
        if (matrixManager.ready()) {
            matrixManager.scale(this, scaleType);
        }
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

    public void setVideoRotation(@IntRange(from = 0, to = 359) int userRotation, @IntRange(from = 0, to = 359) int configurationRotation) {
        requestedUserRotation = userRotation;
        requestedConfigurationRotation = configurationRotation;

        if (matrixManager.ready()) {
            matrixManager.rotate(this, (userRotation + configurationRotation) % MAX_DEGREES);
        }
    }

    protected void updateMatrixOnLayout() {
        synchronized (globalLayoutListenerLock) {
            // if we're not attached defer adding the layout listener until we are
            if(getWindowToken() == null) {
                addGlobalLayoutListenerRequested = true;
                addOnAttachStateChangeListener(attachStateChangeListener);
            } else {
                getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            }
        }
    }

    protected void notifyOnSizeChangeListener(int width, int height) {
        if (lastNotifiedSize.x == width && lastNotifiedSize.y == height) {
            return;
        }

        lastNotifiedSize.x = width;
        lastNotifiedSize.y = height;

        if (onSizeChangeListener != null) {
            onSizeChangeListener.onVideoSurfaceSizeChange(width, height);
        }
    }
}
