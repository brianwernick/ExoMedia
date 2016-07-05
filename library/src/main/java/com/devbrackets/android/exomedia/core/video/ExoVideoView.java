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
import android.os.Build;
import android.util.AttributeSet;

import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;

/**
 * A {@link VideoViewApi} implementation that uses the ExoPlayer
 * as the backing media player.
 *
 * @deprecated {@link ExoTextureVideoView} should be used instead
 */
@Deprecated
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExoVideoView extends ExoTextureVideoView {

    public ExoVideoView(Context context) {
        super(context);
    }

    public ExoVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExoVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
