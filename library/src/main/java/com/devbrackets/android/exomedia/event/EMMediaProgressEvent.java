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
 * changes.  This event will be re-used internally to avoid over-creating objects,
 * if you need to store the current values use
 */
public class EMMediaProgressEvent {
    private static final int MAX_BUFFER_PERCENT = 100;

    private long position;
    private long duration;
    private int bufferPercent;
    private float bufferPercentFloat;

    public EMMediaProgressEvent(long position, int bufferPercent, long duration) {
        update(position, bufferPercent, duration);
    }

    public void update(long position, int bufferPercent, long duration) {
        setPosition(position);
        setBufferPercent(bufferPercent);
        setDuration(duration);
    }

    public long getPosition() {
        if (position < 0) {
            position = 0;
        }

        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        if (duration < 0) {
            duration = 0;
        }

        this.duration = duration;
    }

    public int getBufferPercent() {
        return bufferPercent;
    }

    public void setBufferPercent(int bufferPercent) {
        //Makes sure the bufferPercent is between 0 and 100 inclusive
        if (bufferPercent < 0) {
            bufferPercent = 0;
        }

        if (bufferPercent > MAX_BUFFER_PERCENT) {
            bufferPercent = MAX_BUFFER_PERCENT;
        }

        this.bufferPercent = bufferPercent;
        this.bufferPercentFloat = bufferPercent == MAX_BUFFER_PERCENT ? (float)bufferPercent : (float) bufferPercent / (float) MAX_BUFFER_PERCENT;
    }

    public float getBufferPercentFloat() {
        return bufferPercentFloat;
    }

    /**
     * Obtains a copy of the passed EMMediaProgressEvent
     *
     * @param event The EMMediaProgressEvent to copy
     * @return A copy of the event
     */
    public static EMMediaProgressEvent obtain(EMMediaProgressEvent event) {
        return new EMMediaProgressEvent(event.position, event.bufferPercent, event.duration);
    }
}
