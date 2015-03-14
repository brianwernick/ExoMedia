/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia.event;

import android.media.MediaPlayer;
import android.support.annotation.Nullable;

/**
 * Used to capture when the current media item has a playback error
 * (see {@link android.media.MediaPlayer.OnErrorListener}
 */
public class EMMediaErrorEvent {
    private final MediaPlayer mediaPlayer;
    private final int what;
    private final int extra;

    public EMMediaErrorEvent(@Nullable MediaPlayer mediaPlayer, int what, int extra) {
        this.extra = extra;
        this.what = what;
        this.mediaPlayer = mediaPlayer;
    }

    @Nullable
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getWhat() {
        return what;
    }

    public int getExtra() {
        return extra;
    }
}
