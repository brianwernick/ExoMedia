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

package com.devbrackets.android.exomedia.listener;

import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;

/**
 * A simple callback class to handle the progress events from the
 * {@link com.devbrackets.android.exomedia.EMAudioPlayer} and the
 * {@link com.devbrackets.android.exomedia.EMVideoView}
 */
public interface EMProgressCallback {

    /**
     * Occurs when the currently playing item has a progress change.
     * You can also catch this event with the EventBus event
     * {@link com.devbrackets.android.exomedia.event.EMMediaProgressEvent}
     *
     * @return True if the progress update has been handled
     */
    boolean onProgressUpdated(EMMediaProgressEvent progressEvent);
}
