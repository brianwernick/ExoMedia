package com.devbrackets.android.exomedia.util;

/**
 * An abstraction that allows the user to specify
 * any event bus (e.g. square/Otto, greenrobot/EventBus)
 *
 * @deprecated EventBus support will be removed in the next major release (3.0).
 * Instead the standard listeners should be used
 */
@Deprecated
public interface EMEventBus {
    void post(Object event);
}