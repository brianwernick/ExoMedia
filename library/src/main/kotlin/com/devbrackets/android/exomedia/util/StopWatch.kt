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


package com.devbrackets.android.exomedia.util

import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.FloatRange

/**
 * A simple stopwatch to keep a correct and updated record of the running duration
 * of processes.
 *
 * TODO: can we replace this with fixedRateTimer?
 */
class StopWatch {
  companion object {
    protected const val HANDLER_THREAD_NAME = "ExoMedia_StopWatch_HandlerThread"
    protected const val DEFAULT_TICK_DELAY = 33 // ~30 fps
  }

  /**
   * Determines if the stopwatch is currently running
   *
   * @return True if the stopwatch is currently running
   */
  @Volatile
  var isRunning = false
    protected set

  /**
   * The approximate duration between time updates in milliseconds [default: {@value #DEFAULT_TICK_DELAY}]
   */
  var tickDelay = DEFAULT_TICK_DELAY

  protected var delayedHandler: Handler? = null
  protected val handlerThread by lazy { HandlerThread(HANDLER_THREAD_NAME) }
  protected var useHandlerThread = false

  var tickListener: ((currentTime: Long) -> Unit)? = null
  protected var tickRunnable = TickRunnable()

  protected var startTime: Long = 0
  protected var currentTime: Long = 0
  protected var storedTime: Long = 0

  /**
   * The multiplier to use when calculating the passed duration. This
   * won't affect [tickDelay] but will change the output for the current time.
   */
  @FloatRange(from = 0.0)
  var speedMultiplier = 1f

  /**
   * Retrieves the current time for the stopwatch.  If the stopwatch is stopped then the
   * ending time will be returned.
   *
   * @return The time in milliseconds
   */
  val time: Long
    get() = currentTime + storedTime

  /**
   * @param processOnStartingThread True if the repeating process should be handled on the same thread that created the Repeater
   */
  @JvmOverloads
  constructor(processOnStartingThread: Boolean = true) {
    if (processOnStartingThread) {
      delayedHandler = Handler()
      return
    }

    useHandlerThread = true
  }

  /**
   * @param handler The Handler to use for the repeating process
   */
  constructor(handler: Handler) {
    delayedHandler = handler
  }

  /**
   * Starts the stopwatch.  This will continue from where we last left off,
   * if you need to start from 0 call [.reset] first.
   */
  fun start() {
    if (isRunning) {
      return
    }

    isRunning = true
    startTime = System.currentTimeMillis()
    tickRunnable.lastTickTimestamp = startTime

    if (useHandlerThread) {
      handlerThread.start()
      delayedHandler = Handler(handlerThread.looper)
    }

    tickRunnable.performTick()
  }

  /**
   * Stops the stopwatch, capturing the ending time
   */
  fun stop() {
    if (!isRunning) {
      return
    }

    delayedHandler?.removeCallbacksAndMessages(null)
    handlerThread.quit()

    storedTime += currentTime
    isRunning = false
    currentTime = 0
  }

  /**
   * Resets the current time for the stopWatch
   */
  fun reset() {
    currentTime = 0
    storedTime = 0
    startTime = System.currentTimeMillis()
    tickRunnable.lastTickTimestamp = startTime
  }

  /**
   * Forcefully sets the current time for the stopwatch.
   *
   * @param time The new stopwatch time in milliseconds
   */
  fun overrideCurrentTime(time: Long) {
    startTime = System.currentTimeMillis()
    tickRunnable.lastTickTimestamp = startTime
    currentTime = 0
    storedTime = time
  }

  protected inner class TickRunnable : Runnable {
    protected var tempNow: Long = 0
    var lastTickTimestamp: Long = -1

    override fun run() {
      if (lastTickTimestamp == -1L) {
        lastTickTimestamp = startTime
      }

      tempNow = System.currentTimeMillis()
      currentTime += ((tempNow - lastTickTimestamp) * speedMultiplier).toLong()
      lastTickTimestamp = tempNow

      if (isRunning) {
        performTick()
      }

      tickListener?.invoke(currentTime + storedTime)
    }

    fun performTick() {
      delayedHandler?.postDelayed(tickRunnable, tickDelay.toLong())
    }
  }
}
