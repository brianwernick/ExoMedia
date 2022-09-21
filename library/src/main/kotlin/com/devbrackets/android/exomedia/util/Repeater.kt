package com.devbrackets.android.exomedia.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A method repeater to easily perform update functions on a timed basis.
 * The duration between repeats isn't exact, if you require an exact
 * amount of elapsed time use the [StopWatch] instead.
 */
class Repeater(
  private val delayMillis: Long = DEFAULT_REPEAT_DELAY,
  private val handler: Handler = Handler(Looper.myLooper() ?: Looper.getMainLooper()),
  private val callback: () -> Unit
) {
  companion object {
    private const val DEFAULT_REPEAT_DELAY = 33L // ~30 fps
  }

  private val started = AtomicBoolean(false)
  private var pollRunnable = PollRunnable()

  val running: Boolean
    get() = started.get()

  fun start() {
    if (!started.getAndSet(true)) {
      pollRunnable.performPoll()
    }
  }

  fun stop() {
    started.getAndSet(false)
  }

  private inner class PollRunnable : Runnable {
    override fun run() {
      callback.invoke()

      if (started.get()) {
        performPoll()
      }
    }

    fun performPoll() {
      handler.postDelayed(pollRunnable, delayMillis)
    }
  }
}
