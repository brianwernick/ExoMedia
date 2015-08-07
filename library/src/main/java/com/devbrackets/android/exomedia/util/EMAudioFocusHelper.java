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

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.event.EMAudioFocusGainedEvent;
import com.devbrackets.android.exomedia.event.EMAudioFocusLostEvent;
import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;

/**
 * A helper to simplify audio focus procedures in to simple callbacks and/or
 * EventBus events.
 */
public class EMAudioFocusHelper {
    public enum Focus {
        NONE,               // We haven't tried to obtain focus
        NO_FOCUS_NO_DUCK,   // Don't have focus, and can't duck
        NO_FOCUS_CAN_DUCK,  // don't have focus but can play at low volume ("Ducking")
        FOCUSED             // have full audio focus
    }

    @Nullable
    private EMEventBus bus;
    private AudioManager audioManager;
    private EMAudioFocusCallback callbacks;
    private AudioFocusListener audioFocusListener = new AudioFocusListener();

    private Focus currentFocus = Focus.NONE;

    /**
     * Creates and sets up the basic information for the AudioFocusHelper.  In order to
     * be of any use you must call {@link #setBus(EMEventBus)} or
     * {@link #setAudioFocusCallback(com.devbrackets.android.exomedia.listener.EMAudioFocusCallback)}
     *
     * @param context The context for the AudioFocus (Generally Application)
     */
    public EMAudioFocusHelper(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Sets the bus to use for dispatching Events that correspond to the callbacks
     * listed in {@link com.devbrackets.android.exomedia.listener.EMAudioFocusCallback}
     *
     * @param bus The EventBus to dispatch events on
     */
    public void setBus(@Nullable EMEventBus bus) {
        this.bus = bus;
    }

    /**
     * Sets the AudioFocusCallback to inform of focus changes.
     *
     * @param callback The Callback to inform
     */
    public void setAudioFocusCallback(@Nullable EMAudioFocusCallback callback) {
        this.callbacks = callback;
    }

    /**
     * Retrieves the current audio focus
     *
     * @return The current Focus value currently held
     */
    public Focus getCurrentAudioFocus() {
        return currentFocus;
    }

    /**
     * Requests to obtain the audio focus
     *
     * @return True if the focus was granted
     */
    public boolean requestFocus() {
        if (currentFocus == Focus.FOCUSED) {
            return true;
        }

        int status = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status;
    }

    /**
     * Requests the system to drop the audio focus
     *
     * @return True if the focus was lost
     */
    public boolean abandonFocus() {
        if (currentFocus == Focus.NONE) {
            return true;
        }

        int status = audioManager.abandonAudioFocus(audioFocusListener);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
            currentFocus = Focus.NONE;
        }

        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status;
    }

    private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    currentFocus = Focus.FOCUSED;
                    postAudioFocusGained();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    currentFocus = Focus.NO_FOCUS_NO_DUCK;
                    postAudioFocusLost(false);
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    currentFocus = Focus.NO_FOCUS_CAN_DUCK;
                    postAudioFocusLost(true);
                    break;

                default:
                    break;
            }
        }

        private void postAudioFocusGained() {
            if (callbacks != null && callbacks.onAudioFocusGained()) {
                return;
            }

            if (bus != null) {
                bus.post(new EMAudioFocusGainedEvent());
            }
        }

        private void postAudioFocusLost(boolean canDuck) {
            if (callbacks != null && callbacks.onAudioFocusLost(canDuck)) {
                return;
            }

            if (bus != null) {
                bus.post(new EMAudioFocusLostEvent(canDuck));
            }
        }
    }
}
