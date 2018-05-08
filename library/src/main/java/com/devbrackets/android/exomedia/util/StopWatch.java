/*
 * Copyright (C) 2016 - 2018 ExoMedia Contributors
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
import android.os.HandlerThread;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;

/**
 * A simple stopwatch to keep a correct and updated record of the running duration
 * of processes.
 */
public class StopWatch {
    protected static final String HANDLER_THREAD_NAME = "ExoMedia_StopWatch_HandlerThread";
    protected static final int DEFAULT_TICK_DELAY = 33; // ~30 fps

    public interface TickListener {
        void onStopWatchTick(long currentTime);
    }

    protected volatile boolean isRunning = false;
    protected int tickDelay = DEFAULT_TICK_DELAY;

    protected Handler delayedHandler;
    protected HandlerThread handlerThread;
    protected boolean useHandlerThread = false;

    protected TickListener listener;
    protected TickRunnable tickRunnable = new TickRunnable();

    protected long startTime = 0;
    protected long currentTime = 0;
    protected long storedTime = 0;
    @FloatRange(from = 0F)
    protected float speedMultiplier = 1F;

    public StopWatch() {
        this(true);
    }

    /**
     * @param processOnStartingThread True if the repeating process should be handled on the same thread that created the Repeater
     */
    public StopWatch(boolean processOnStartingThread) {
        if (processOnStartingThread) {
            delayedHandler = new Handler();
            return;
        }

        useHandlerThread = true;
    }

    /**
     * @param handler The Handler to use for the repeating process
     */
    public StopWatch(Handler handler) {
        delayedHandler = handler;
    }

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
     * Sets the multiplier to use when calculating the passed duration. This
     * won't affect the tick delay {@link #setTickDelay(int)} but will change
     * the output for the current time.
     *
     * @param multiplier The amount to multiply the duration between each tick by
     */
    public void setSpeedMultiplier(@FloatRange(from = 0F) float multiplier) {
        speedMultiplier = multiplier;
    }

    /**
     * Retrieves the current multiplier used for the current time calculations.
     * NOTE: the default is 1
     *
     * @return The current multiplier
     */
    @FloatRange(from = 0F)
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Starts the stopwatch.  This will continue from where we last left off,
     * if you need to start from 0 call {@link #reset()} first.
     */
    public void start() {
        if (isRunning()) {
            return;
        }

        isRunning = true;
        startTime = System.currentTimeMillis();
        tickRunnable.lastTickTimestamp = startTime;

        if (useHandlerThread) {
            handlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            handlerThread.start();
            delayedHandler = new Handler(handlerThread.getLooper());
        }

        tickRunnable.performTick();
    }

    /**
     * Stops the stopwatch, capturing the ending time
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }

        delayedHandler.removeCallbacksAndMessages(null);
        if (handlerThread != null) {
            handlerThread.quit();
        }

        storedTime = currentTime + storedTime;
        isRunning = false;
        currentTime = 0;
    }

    /**
     * Resets the current time for the stopWatch
     */
    public void reset() {
        currentTime = 0;
        storedTime = 0;
        startTime = System.currentTimeMillis();
        tickRunnable.lastTickTimestamp = startTime;
    }

    /**
     * Forcefully sets the current time for the stopwatch.
     *
     * @param time The new stopwatch time in milliseconds
     */
    public void overrideCurrentTime(long time) {
        startTime = System.currentTimeMillis();
        tickRunnable.lastTickTimestamp = startTime;
        currentTime = 0;
        storedTime = time;
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
        return currentTime + storedTime;
    }

    /**
     * Retrieves the current time for the stopwatch.  If the stopwatch is stopped then the
     * ending time will be returned.
     *
     * @return The time in milliseconds
     */
    public int getTimeInt() {
        long time = currentTime + storedTime;
        return time < Integer.MAX_VALUE ? (int) time : Integer.MAX_VALUE;
    }

    /**
     * Sets the listener to be notified for each time update (tick)
     *
     * @param listener The listener or null
     */
    public void setTickListener(@Nullable TickListener listener) {
        this.listener = listener;
    }

    protected class TickRunnable implements Runnable {
        protected long tempNow = 0;
        protected long lastTickTimestamp = -1;

        @Override
        public void run() {
            if (lastTickTimestamp == -1L) {
                lastTickTimestamp = startTime;
            }

            tempNow = System.currentTimeMillis();
            currentTime += (tempNow - lastTickTimestamp) * speedMultiplier;
            lastTickTimestamp = tempNow;

            if (isRunning) {
                performTick();
            }

            if (listener != null) {
                listener.onStopWatchTick(currentTime + storedTime);
            }
        }

        public void performTick() {
            delayedHandler.postDelayed(tickRunnable, tickDelay);
        }
    }
}
