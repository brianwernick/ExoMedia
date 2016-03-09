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
 * Basic VideoView callbacks.  These can also be accessed through
 * their corresponding EventBus events.
 */
public interface VideoControlsListener {
    /**
     * Occurs when the PlayPause button on the VideoView is clicked
     *
     * @return True if the event has been handled
     */
    boolean onPlayPauseClicked();

    /**
     * Occurs when the Previous button on the VideoView is clicked
     *
     * @return True if the event has been handled
     */
    boolean onPreviousClicked();

    /**
     * Occurs when the Next button on the VideoView is clicked
     *
     * @return True if the event has been handled
     */
    boolean onNextClicked();

    /**
     * Occurs when the DefaultControls becomes visible
     *
     * @return True if the event has been handled
     */
    boolean onControlsShown();

    /**
     * Occurs when the DefaultControls becomes hidden
     *
     * @return True if the event has been handled
     */
    boolean onControlsHidden();
}