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
 * A simple stopwatch to keep a correct and updated record of the running duration
 * of processes.
 */
public class StopWatch {
    private static final int DEFAULT_TICK_DELAY = 33; // ~30 fps

    private volatile boolean isRunning = false;
    private int tickDelay = DEFAULT_TICK_DELAY;
    private Handler delayedHandler = new Handler();

    private TickListener listener;
    private TickRunnable tickRunnable = new TickRunnable();

    private long startTime = 0;
    private long time = 0;

    /**
     * Sets the approximate duration between time updates.
     *
     * @param milliSeconds The approximate duration between time updates [default: {@value #DEFAULT_TICK_DELAY}]
     */
    public void setTickDelay(int milliSeconds) {
        tickDelay = milliSeconds;
    }

    /**
     * Retrieves the approximate duration between time updates.
     *
     * @return The approximate duration in milliseconds between time updates
     */
    public int getTickDelay() {
        return tickDelay;
    }

    /**
     * Starts the stopwatch.  This will reset the time to 0
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            reset();
            tickRunnable.performTick();
        }
    }

    /**
     * If the stopwatch is currently running then the time will be reset to 0.
     */
    public void reset() {
        if (!isRunning) {
            return;
        }

        time = 0;
        startTime = System.currentTimeMillis();
    }

    /**
     * Stops the stopwatch, capturing the ending time
     */
    public void stop() {
        isRunning = false;
        time = System.currentTimeMillis() - startTime;
    }

    /**
     * Determines if the stopwatch is currently running
     *
     * @return True if the stopwatch is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Retrieves the current time for the stopwatch.  If the stopwatch is stopped then the
     * ending time will be returned.
     *
     * @return The time in milliseconds
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the listener to be notified for each time update (tick)
     *
     * @param listener The listener or null
     */
    public void setTickListener(@Nullable TickListener listener) {
        this.listener = listener;
    }

    public interface TickListener {
        void onStopWatchTick(long currentTime);
    }

    private class TickRunnable implements Runnable {
        @Override
        public void run() {
            time = System.currentTimeMillis() - startTime;

            if (isRunning) {
                performTick();
            }

            if (listener != null) {
                listener.onStopWatchTick(time);
            }
        }

        public void performTick() {
            delayedHandler.postDelayed(tickRunnable, tickDelay);
        }
    }
}
