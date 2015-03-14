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

package com.devbrackets.android.exomedia.util;

import android.os.Handler;
import android.support.annotation.Nullable;

/**
 * A method repeater to easily perform update functions on a timed basis.
 * <b>NOTE:</b> the duration between repeats may not be exact.  If you require an exact
 * amount of elapsed time use the {@link StopWatch} instead.
 */
public class Repeater {
    private static final int DEFAULT_REPEAT_DELAY = 33; // ~30 fps

    private volatile boolean repeaterRunning = false;
    private int repeatDelay = DEFAULT_REPEAT_DELAY;
    private Handler delayedHandler = new Handler();

    private RepeatListener listener;
    private PollRunnable pollRunnable = new PollRunnable();

    /**
     * Sets the amount of time between method invocation.
     *
     * @param milliSeconds The time between method calls [default: {@value #DEFAULT_REPEAT_DELAY}]
     */
    public void setRepeaterDelay(int milliSeconds) {
        repeatDelay = milliSeconds;
    }

    /**
     * Retrieves the amount of time between method invocation.
     *
     * @return The millisecond time between method calls
     */
    public int getRepeaterDelay() {
        return repeatDelay;
    }

    /**
     * Starts the repeater
     */
    public void start() {
        if (!repeaterRunning) {
            repeaterRunning = true;
            pollRunnable.performPoll();
        }
    }

    /**
     * Stops the repeater
     */
    public void stop() {
        repeaterRunning = false;
    }

    /**
     * Determines if the Repeater is currently running
     *
     * @return True if the repeater is currently running
     */
    public boolean isRunning() {
        return repeaterRunning;
    }

    /**
     * Sets the listener to be notified for each repeat
     *
     * @param listener The listener or null
     */
    public void setRepeatListener(@Nullable RepeatListener listener) {
        this.listener = listener;
    }

    public interface RepeatListener {
        void onRepeat();
    }

    private class PollRunnable implements Runnable {
        @Override
        public void run() {
            if (repeaterRunning) {
                performPoll();
            }

            if (listener != null) {
                listener.onRepeat();
            }
        }

        public void performPoll() {
            delayedHandler.postDelayed(pollRunnable, repeatDelay);
        }
    }
}
