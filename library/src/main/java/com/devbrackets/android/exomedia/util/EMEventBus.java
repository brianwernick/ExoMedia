package com.devbrackets.android.exomedia.util;

/**
 * An abstraction that allows the user to specify
 * any event bus (e.g. square/Otto, greenrobot/EventBus)
 */
public interface EMEventBus {
    void post(Object event);
}