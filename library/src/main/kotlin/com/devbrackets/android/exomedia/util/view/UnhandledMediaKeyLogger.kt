package com.devbrackets.android.exomedia.util.view

import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.annotation.RequiresApi

/**
 * Handles logging unhandled [KeyEvent]s, filtering out duplicates when
 * possible.
 */
class UnhandledMediaKeyLogger {
  private val activeEvents = mutableMapOf<String, KeyEvent>()

  private val listener: Listener? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      Listener(this::onUnhandledKeyEvent)
    } else {
      null
    }
  }

  /**
   * Uses the [view] to register for unhandled [KeyEvent]s when possible. The registration will automatically
   * be removed when the [view] is detached from it's parent.
   *
   * NOTE: Unhandled [KeyEvent]s are only available on API 28+ (Version Code P)
   */
  fun register(view: View) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return
    }

    listener?.let {
      view.addOnUnhandledKeyEventListener(it)
      view.addOnAttachStateChangeListener(ViewDetachedStateChangeListener { listener, view ->
        view.removeOnAttachStateChangeListener(listener)
        view.removeOnUnhandledKeyEventListener(it)
      })
    }
  }

  private fun onUnhandledKeyEvent(v: View?, event: KeyEvent?): Boolean {
    if (event == null) {
      return false
    }

    logUniqueEvent(v, event)
    return false
  }

  private fun logUniqueEvent(view: View?, event: KeyEvent) {
    @Suppress("DEPRECATION")
    if (event.action == KeyEvent.ACTION_MULTIPLE) {
      return
    }

    val eventKey = getEventKey(view, event)
    val activeEvent = activeEvents[eventKey]

    if (activeEvent != null) {
      if (activeEvent.action == event.action) {
        return
      } else {
        activeEvents.remove(eventKey)
      }
    } else if (event.action == KeyEvent.ACTION_DOWN) {
      activeEvents[eventKey] = event
    }

    Log.d("UnhandledKey", "KeyEvent (${getKeyName(event)}, ${getActionName(event)}) @ ${event.eventTime} for view ${view?.javaClass?.simpleName ?: "null"} wasn't handled")
  }

  private fun getEventKey(view: View?, event: KeyEvent): String {
    if (view == null) {
      return event.keyCode.toString()
    }

    val viewKeyPartial = if (view.id != View.NO_ID) {
      view.id.toString()
    } else {
      System.identityHashCode(view).toString()
    }

    return "$viewKeyPartial-${event.keyCode}"
  }

  @Suppress("DEPRECATION")
  private fun getActionName(event: KeyEvent): String {
    return when(event.action) {
      KeyEvent.ACTION_DOWN -> "Key Down"
      KeyEvent.ACTION_UP -> "Key Up"
      KeyEvent.ACTION_MULTIPLE -> "Key Multiple"
      else -> "Unknown Key Action"
    }
  }

  private fun getKeyName(event: KeyEvent): String {
    return when(event.keyCode) {
      KeyEvent.KEYCODE_DPAD_LEFT -> "DPad Left"
      KeyEvent.KEYCODE_DPAD_RIGHT -> "DPad Right"
      KeyEvent.KEYCODE_DPAD_UP -> "DPad Up"
      KeyEvent.KEYCODE_DPAD_DOWN -> "DPad Down"
      else -> "KeyCode ${event.keyCode}"
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private class Listener(
    private val callback: (View?, KeyEvent?) -> Boolean
  ): View.OnUnhandledKeyEventListener {
    override fun onUnhandledKeyEvent(v: View?, event: KeyEvent?): Boolean {
      return callback(v, event)
    }
  }

  private class ViewDetachedStateChangeListener(
    private val onViewDetached: (listener: ViewDetachedStateChangeListener, view: View) -> Unit
  ): OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) {
      // No-Op
    }

    override fun onViewDetachedFromWindow(v: View) {
      onViewDetached(this, v)
    }
  }
}