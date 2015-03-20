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

import com.devbrackets.android.exomedia.util.EMAudioFocusHelper;

/**
 * Used to capture when the audio focus is lost.  This can also be accessed
 * through the callbacks {@link com.devbrackets.android.exomedia.listener.EMAudioFocusCallback} when
 * registered in the {@link EMAudioFocusHelper}.  Additionally, if the callbacks
 * are implemented and consume the event, this will NOT be called.
 */
public class EMAudioFocusLostEvent {
    private final boolean canDuck;

    public EMAudioFocusLostEvent(boolean canDuck) {
        this.canDuck = canDuck;
    }

    public boolean canDuck() {
        return canDuck;
    }
}
