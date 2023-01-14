package com.devbrackets.android.exomedia.util.view

import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener

/**
 * An implementation of the [View.OnKeyListener] that delegates to registered listeners
 * to support multiple listeners without overwriting each other. This allows the creation
 * of listeners with a single responsibility while adding minimal overhead.
 */
open class DelegatedOnKeyListener: OnKeyListener {
  protected val delegates = mutableListOf<OnKeyListener>()

  override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
    return delegates.any { delegate ->
      delegate.onKey(v, keyCode, event)
    }
  }

  fun register(listener: OnKeyListener) {
    delegates.add(listener)
  }

  fun unRegister(listener: OnKeyListener): Boolean {
    return delegates.remove(listener)
  }

  fun unRegisterAll() {
    delegates.clear()
  }
}