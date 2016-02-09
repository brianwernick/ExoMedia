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

package com.devbrackets.android.exomedia.core.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.VideoView;

import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.type.VideoViewApi;

/**
 * A {@link VideoViewApi} implementation that uses the
 * standard Android VideoView as an implementation
 */
public class NativeVideoView extends VideoView implements VideoViewApi {
    private OnTouchListener touchListener;
    protected EMListenerMux listenerMux;

    public NativeVideoView(Context context) {
        super(context);
        setup();
    }

    public NativeVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public NativeVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NativeVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean flag = false;
        if (touchListener != null) {
            flag = touchListener.onTouch(this, ev);
        }

        return flag || super.onTouchEvent(ev);
    }

    @Override
    public void setOnTouchListener(OnTouchListener listener) {
        touchListener = listener;
        super.setOnTouchListener(listener);
    }

    @Override
    public void setVideoUri(@Nullable Uri uri) {
        setVideoUri(uri, null);
    }

    @Override
    public void setVideoUri(@Nullable Uri uri, @Nullable RenderBuilder renderBuilder) {
        super.setVideoURI(uri);
    }

    @Override
    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        return false;
    }

    @Override
    public int getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return super.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return super.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return getBufferPercentage();
    }

    @Override
    public void start() {
        super.start();
        listenerMux.setNotifiedCompleted(false);
    }

    @Override
    public void release() {
        //Purposefully left blank
    }

    @Override
    public void setListenerMux(EMListenerMux listenerMux) {
        this.listenerMux = listenerMux;

        setOnCompletionListener(listenerMux);
        setOnPreparedListener(listenerMux);
        setOnErrorListener(listenerMux);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setOnInfoListener(listenerMux);
        }
    }

    @Override
    public void updateAspectRatio(float aspectRatio) {
        //Purposefully left blank
    }

    @Override
    public void setOnSizeChangedListener(@Nullable OnSurfaceSizeChanged listener) {
        //Purposefully left blank
    }

    protected void setup() {
        //Purposefully left blank
    }
}
