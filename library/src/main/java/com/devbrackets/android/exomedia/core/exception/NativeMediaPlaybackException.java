/*
 * Copyright (C) 2017 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.exception;

import android.media.MediaPlayer;

/**
 * An Exception to indicate that the Native {@link MediaPlayer} has
 * had an error during or while attempting playback
 */
public class NativeMediaPlaybackException extends Exception {
    public final int what;
    public final int extra;

    public NativeMediaPlaybackException(int what, int extra) {
        super(MediaPlayer.class.getName() + " has had the error " + what + " with extras " + extra);

        this.what = what;
        this.extra = extra;
    }
}
