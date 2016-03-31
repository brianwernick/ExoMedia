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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewTreeObserver;

import com.devbrackets.android.exomedia.core.video.scale.MatrixManager;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;

/**
 * A VideoTextureView that reSizes itself to match a specified aspect
 * ratio for videos.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AspectTextureView extends TextureView {
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

    public interface OnSizeChangeListener {
        void onVideoSurfaceSizeChange(int width, int height);
    }

    private float videoAspectRatio;

    @Nullable
    private OnSizeChangeListener sizeChangeListener;
    private Point oldSize = new Point(0, 0);

    @NonNull
    protected ScaleType currentScaleType = ScaleType.CENTER_INSIDE;
    protected MatrixManager matrixManager = new MatrixManager();

    public AspectTextureView(Context context) {
        super(context);
    }

    public AspectTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AspectTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
            onSizeChange(width, height);
            return;
        }

        if (aspectDeformation > 0) {
            height = (int) (width / videoAspectRatio);
        } else {
            width = (int) (height * videoAspectRatio);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        onSizeChange(width, height);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        updateScaleOnLayout();
        super.onConfigurationChanged(newConfig);
    }

    public void setOnSizeChangeListener(@Nullable OnSizeChangeListener listener) {
        this.sizeChangeListener = listener;
    }

    /**
     * Set the aspect ratio that this {@link AspectTextureView} should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    public void updateIntrinsicVideoSize(int width, int height) {
        matrixManager.setIntrinsicVideoSize(width, height);
        updateScaleOnLayout();
    }

    public void setScaleType(@NonNull ScaleType scaleType) {
        currentScaleType = scaleType;
        if (matrixManager.ready()) {
            matrixManager.scale(this, scaleType);
        }
    }

    protected void updateScaleOnLayout() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setScaleType(currentScaleType);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void onSizeChange(int width, int height) {
        if (oldSize.x == width && oldSize.y == height) {
            return;
        }

        oldSize.x = width;
        oldSize.y = height;

        if (sizeChangeListener != null) {
            sizeChangeListener.onVideoSurfaceSizeChange(width, height);
        }
    }
}
