package com.devbrackets.android.exomedia.util

import android.os.Handler
import android.os.HandlerThread

/**
 * A method repeater to easily perform update functions on a timed basis.
 * **NOTE:** the duration between repeats may not be exact.  If you require an exact
 * amount of elapsed time use the [StopWatch] instead.
 *
 * TODO: Replace with FixedRateTimer?
 */
class Repeater {
  companion object {
    protected const val HANDLER_THREAD_NAME = "ExoMedia_Repeater_HandlerThread"
    protected const val DEFAULT_REPEAT_DELAY = 33 // ~30 fps
  }

  /**
   * 'true' if the repeater is currently running
   */
  @Volatile
  var isRunning = false
    protected set

  /**
   * The time in milliseconds between method calls (default: [DEFAULT_REPEAT_DELAY])
   */
  var repeaterDelay = DEFAULT_REPEAT_DELAY

  protected var delayedHandler: Handler? = null
  protected val handlerThread by lazy { HandlerThread(HANDLER_THREAD_NAME) }
  protected var useHandlerThread = false

  var repeatListener: (() -> Unit)? = null
  protected var pollRunnable = PollRunnable()


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
   * Starts the repeater
   */
  fun start() {
    if (!isRunning) {
      isRunning = true

      if (useHandlerThread) {
        handlerThread.start()
        delayedHandler = Handler(handlerThread.looper)
      }

      pollRunnable.performPoll()
    }
  }

  /**
   * Stops the repeater
   */
  fun stop() {
    handlerThread.quit()
    isRunning = false
  }

  protected inner class PollRunnable : Runnable {
    override fun run() {
      repeatListener?.invoke()

      if (isRunning) {
        performPoll()
      }
    }

    fun performPoll() {
      delayedHandler?.postDelayed(pollRunnable, repeaterDelay.toLong())
    }
  }
}
