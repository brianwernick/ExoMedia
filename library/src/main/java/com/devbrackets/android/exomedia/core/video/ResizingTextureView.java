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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * A TextureView that reSizes itself according to the requested layout type
 * once we have a video
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ResizingTextureView extends TextureView {
    public interface OnSizeChangeListener {
        void onVideoSurfaceSizeChange(int width, int height);
    }

    @Nullable
    protected OnSizeChangeListener onSizeChangeListener;
    @NonNull
    protected Point lastNotifiedSize = new Point(0, 0);
    @NonNull
    protected Point videoSize = new Point(0, 0);

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

    protected void notifyOnSizeChangeListener(int width, int height) {
        if (onSizeChangeListener != null && (lastNotifiedSize.x != width || lastNotifiedSize.y != height)) {
            lastNotifiedSize.x = width;
            lastNotifiedSize.y = height;
            onSizeChangeListener.onVideoSurfaceSizeChange(width, height);
        }
    }
}
