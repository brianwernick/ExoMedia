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

/**
 * An event to be used to inform listeners of media (e.g. audio, video) progress
 * changes.
 */
public class EMMediaProgressEvent {
    private static final int MAX_BUFFER_PERCENT = 100;

    private final long position;
    private final long duration;
    private final int bufferPercent;
    private final float bufferPercentFloat;

    public EMMediaProgressEvent(long position, int bufferPercent, long duration) {
        if (position < 0) {
            position = 0;
        }

        //Makes sure the bufferPercent is between 0 and 100 inclusive
        if (bufferPercent < 0) {
            bufferPercent = 0;
        }

        if (bufferPercent > MAX_BUFFER_PERCENT) {
            bufferPercent = MAX_BUFFER_PERCENT;
        }

        if (duration < 0) {
            duration = 0;
        }

        this.position = position;
        this.duration = duration;
        this.bufferPercent = bufferPercent;
        this.bufferPercentFloat = (float) bufferPercent / (float) MAX_BUFFER_PERCENT;
    }

    public long getPosition() {
        return position;
    }

    public int getBufferPercent() {
        return bufferPercent;
    }

    public float getBufferPercentFloat() {
        return bufferPercentFloat;
    }

    public long getDuration() {
        return duration;
    }
}
