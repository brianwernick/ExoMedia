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

import java.io.Serializable;

public class MediaControlsReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaControlsReceiver";

    private boolean intentsCreated = false;
    private PendingIntent playPausePendingIntent, nextPendingIntent, previousPendingIntent;
    private Class<? extends Service> mediaServiceClass;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }

        //Retrieves the class to inform of media button clicks, and creates the pending intents that will perform the actual
        // notifications.
        if (!intentsCreated) {
            if (mediaServiceClass == null) {
                Serializable serializableClass = intent.getSerializableExtra(EMLockScreen.RECEIVER_EXTRA_CLASS);
                if (serializableClass != null && serializableClass instanceof Class) {
                    //noinspection unchecked
                    mediaServiceClass = (Class<? extends Service>)serializableClass;
                }
            }

            //Creates the actual pending intents
            if (mediaServiceClass != null) {
                createIntents(context);
                intentsCreated = true;
            }
        }


        //Performs the actual handling of the button events
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (intentsCreated && event != null && event.getAction() == KeyEvent.ACTION_UP) {
            handleKeyEvent(event);
        }
    }

    /**
     * Creates the PendingIntents that inform the mediaService of button clicks
     */
    private void createIntents(Context context) {
        playPausePendingIntent = createPendingIntent(context, EMRemoteActions.ACTION_PLAY_PAUSE, mediaServiceClass);
        nextPendingIntent = createPendingIntent(context, EMRemoteActions.ACTION_NEXT, mediaServiceClass);
        previousPendingIntent = createPendingIntent(context, EMRemoteActions.ACTION_PREVIOUS, mediaServiceClass);
    }

    /**
     * Handles the media button click events
     *
     * @param keyEvent The KeyEvent associated with the button click
     */
    private void handleKeyEvent(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                sendPendingIntent(playPausePendingIntent);
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                sendPendingIntent(nextPendingIntent);
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                sendPendingIntent(previousPendingIntent);
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

    private void sendPendingIntent(PendingIntent pi) {
        try {
            pi.send();
        } catch (Exception e) {
            Log.d(TAG, "Error sending lock screen pending intent", e);
        }
    }
}
