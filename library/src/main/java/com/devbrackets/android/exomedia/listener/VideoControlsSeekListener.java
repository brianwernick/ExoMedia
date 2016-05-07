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

package com.devbrackets.android.exomedia.listener;

/**
 * Interface definition for a callback to be invoked when the media
 * has external seek requests
 */
public interface VideoControlsSeekListener {

    /**
     * Occurs when the media has entered a state of waiting until the
     * {@link #onSeekEnded(int)} is called.
     *
     * @return {@code true} if the state has been handled
     */
    boolean onSeekStarted();

    /**
     * Occurs when the media has finalized the request to seek.  This
     * may be called after {@link #onSeekStarted()} or on its own.
     *
     * @param seekTime The time in milliseconds to seek to
     * @return {@code true} if the seek has been handled
     */
    boolean onSeekEnded(int seekTime);
}
