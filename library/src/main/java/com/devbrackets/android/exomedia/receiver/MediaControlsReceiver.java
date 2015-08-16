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

package com.devbrackets.android.exomedia.receiver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.devbrackets.android.exomedia.EMLockScreen;
import com.devbrackets.android.exomedia.EMRemoteActions;

/**
 * A Receiver to handle lock screen and other remote controls
 * such as Bluetooth devices
 */
public class MediaControlsReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaControlsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }

        //Retrieves the class to inform of media button clicks
        Class<? extends Service> mediaServiceClass = null;
        String className = intent.getStringExtra(EMLockScreen.RECEIVER_EXTRA_CLASS);
        if (className != null) {
            try {
                //noinspection unchecked
                mediaServiceClass = (Class<? extends Service>) Class.forName(className);
            } catch (Exception e) {
                //Purposefully left blank
            }
        }

        //Informs the mediaService of the event
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (mediaServiceClass != null && event != null && event.getAction() == KeyEvent.ACTION_UP) {
            handleKeyEvent(context, mediaServiceClass, event);
        }
    }

    /**
     * Handles the media button click events
     *
     * @param context The context to use when informing the media service of the event
     * @param mediaServiceClass The service class to inform of the event
     * @param keyEvent The KeyEvent associated with the button click
     */
    private void handleKeyEvent(Context context, Class<? extends Service> mediaServiceClass, KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                sendPendingIntent(createPendingIntent(context, EMRemoteActions.ACTION_PLAY_PAUSE, mediaServiceClass));
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                sendPendingIntent(createPendingIntent(context, EMRemoteActions.ACTION_NEXT, mediaServiceClass));
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                sendPendingIntent(createPendingIntent(context, EMRemoteActions.ACTION_PREVIOUS, mediaServiceClass));
                break;

            default:
                break;
        }
    }

    /**
     * Creates a PendingIntent for the given action to the specified service
     *
     * @param action The action to use
     * @param serviceClass The service class to notify of intents
     * @return The resulting PendingIntent
     */
    private PendingIntent createPendingIntent(Context context, String action, Class<? extends Service> serviceClass) {
        Intent intent = new Intent(context, serviceClass);
        intent.setAction(action);

        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Attempts to send the pending intent
     *
     * @param pi The pending intent to send
     */
    private void sendPendingIntent(PendingIntent pi) {
        try {
            pi.send();
        } catch (Exception e) {
            Log.d(TAG, "Error sending lock screen pending intent", e);
        }
    }
}
