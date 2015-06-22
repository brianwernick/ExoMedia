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

package com.devbrackets.android.exomedia;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A class to help simplify notification creation and modification for
 * media playback applications.
 */
public class EMNotification {
    private Context context;
    private NotificationManager notificationManager;
    private NotificationInfo notificationInfo = new NotificationInfo();

    private Class<? extends Service> mediaServiceClass;
    private RemoteViews bigContent;

    public EMNotification(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Sets weather notifications are shown when audio is playing or
     * ready for playback (e.g. paused).  The notification information
     * will need to be updated by calling {@link #setNotificationBaseInformation(int, int)}
     * and {@link #updateNotificationInformation(String, String, Bitmap, Bitmap)} and can be retrieved
     * with {@link #getNotification(android.app.PendingIntent)}
     *
     * @param enabled True if notifications should be shown
     */
    public void setNotificationsEnabled(boolean enabled) {
        if (enabled == notificationInfo.getShowNotifications()) {
            return;
        }

        notificationInfo.setShowNotifications(enabled);

        //Remove the notification when disabling
        if (!enabled) {
            notificationManager.cancel(notificationInfo.getNotificationId());
        }
    }

    /**
     * Sets the basic information for the notification that doesn't need to be updated.  To enable the big
     * notification you will need to use {@link #setNotificationBaseInformation(int, int, Class)} instead
     *
     * @param notificationId The ID to specify this notification
     * @param appIcon The applications icon resource
     */
    public void setNotificationBaseInformation(int notificationId, @DrawableRes int appIcon) {
        setNotificationBaseInformation(notificationId, appIcon, null);
    }

    /**
     * Sets the basic information for the notification that doesn't need to be updated.  Additionally, when
     * the mediaServiceClass is set the big notification will send intents to that service to notify of
     * button clicks.  These intents will have an action from
     * <ul>
     *     <li>{@link EMRemoteActions#ACTION_STOP}</li>
     *     <li>{@link EMRemoteActions#ACTION_PLAY_PAUSE}</li>
     *     <li>{@link EMRemoteActions#ACTION_PREVIOUS}</li>
     *     <li>{@link EMRemoteActions#ACTION_NEXT}</li>
     * </ul>
     *
     * @param notificationId The ID to specify this notification
     * @param appIcon The applications icon resource
     * @param mediaServiceClass The class for the service to notify of big notification actions
     */
    public void setNotificationBaseInformation(int notificationId, @DrawableRes int appIcon, @Nullable Class<? extends Service> mediaServiceClass) {
        notificationInfo.setNotificationId(notificationId);
        notificationInfo.setAppIcon(appIcon);
        this.mediaServiceClass = mediaServiceClass;
    }

    /**
     * Sets the volatile information for the notification.  This information is expected to
     * change frequently.
     *
     * @param title The title to display for the notification (e.g. A song name)
     * @param content A short description or additional information for the notification (e.g. An artists name)
     * @param notificationImage An image to display on the notification (e.g. Album artwork)
     * @param secondaryNotificationImage An image to display on the notification should be used to indicate playback type (e.g. Chromecast)
     */
    public void updateNotificationInformation(String title, String content, @Nullable Bitmap notificationImage, @Nullable Bitmap secondaryNotificationImage) {
        updateNotificationInformation(title, content, notificationImage, secondaryNotificationImage, null);
    }

    /**
     * Sets the volatile information for the notification.  This information is expected to
     * change frequently.
     *
     * @param title The title to display for the notification (e.g. A song name)
     * @param content A short description or additional information for the notification (e.g. An artists name)
     * @param notificationImage An image to display on the notification (e.g. Album artwork)
     * @param secondaryNotificationImage An image to display on the notification should be used to indicate playback type (e.g. Chromecast)
     * @param notificationMediaState The current media state for the expanded (big) notification
     */
    public void updateNotificationInformation(String title, String content, @Nullable Bitmap notificationImage, @Nullable Bitmap secondaryNotificationImage,
                                              @Nullable NotificationMediaState notificationMediaState) {
        notificationInfo.setTitle(title);
        notificationInfo.setContent(content);
        notificationInfo.setLargeImage(notificationImage);
        notificationInfo.setSecondaryImage(secondaryNotificationImage);
        notificationInfo.setMediaState(notificationMediaState);

        if (notificationInfo.getShowNotifications()) {
            notificationManager.notify(notificationInfo.getNotificationId(), getNotification(notificationInfo.getPendingIntent()));
        }
    }

    /**
     * Returns a fully constructed notification to use when moving a service to the
     * foreground.  This should be called after the notification information is set with
     * {@link #setNotificationBaseInformation(int, int)} and {@link #updateNotificationInformation(String, String, Bitmap, Bitmap)}.
     *
     * @param pendingIntent The pending intent to use when the notification itself is clicked
     * @return The constructed notification
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Notification getNotification(@Nullable PendingIntent pendingIntent) {
        notificationInfo.setPendingIntent(pendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(notificationInfo.getTitle());
        builder.setContentText(notificationInfo.getContent());

        builder.setSmallIcon(notificationInfo.getAppIcon());
        builder.setLargeIcon(notificationInfo.getLargeImage());
        builder.setOngoing(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        //Set the notification category on lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        //Build the notification and set the expanded content view if there is a service to inform of clicks
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && mediaServiceClass != null) {
            notification.bigContentView = getBigNotification();
        }

        return notification;
    }

    /**
     * Creates the RemoteViews used for the expanded (big) notification
     *
     * @return The resulting RemoteViews
     */
    private RemoteViews getBigNotification() {
        if (bigContent == null) {
            bigContent = new RemoteViews(context.getPackageName(), R.layout.exomedia_big_notification_content);

            bigContent.setOnClickPendingIntent(R.id.exomedia_notification_close, createPendingIntent(EMRemoteActions.ACTION_STOP, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.exomedia_notification_playpause, createPendingIntent(EMRemoteActions.ACTION_PLAY_PAUSE, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.exomedia_notification_next, createPendingIntent(EMRemoteActions.ACTION_NEXT, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.exomedia_notification_prev, createPendingIntent(EMRemoteActions.ACTION_PREVIOUS, mediaServiceClass));
        }

        bigContent.setTextViewText(R.id.exomedia_notification_title, notificationInfo.getTitle());
        bigContent.setTextViewText(R.id.exomedia_notification_content_text, notificationInfo.getContent());
        bigContent.setBitmap(R.id.exomedia_notification_large_image, "setImageBitmap", notificationInfo.getLargeImage());
        bigContent.setBitmap(R.id.exomedia_notification_secondary_image, "setImageBitmap", notificationInfo.getSecondaryImage());

        //Makes sure the play/pause, next, and previous are displayed correctly
        if (notificationInfo.getMediaState() != null) {
            updateMediaState(bigContent);
        }

        return bigContent;
    }

    /**
     * Updates the images for the play/pause, next, and previous buttons so that only valid ones are
     * displayed with the correct state.
     *
     * @param bigContent The RemoteViews to use to modify the state
     */
    private void updateMediaState(RemoteViews bigContent) {
        NotificationMediaState state = notificationInfo.getMediaState();
        if (bigContent == null || state == null) {
            return;
        }

        bigContent.setImageViewResource(R.id.exomedia_notification_playpause, state.isPlaying() ? R.drawable.exomedia_notification_pause
                : R.drawable.exomedia_notification_play);

        bigContent.setInt(R.id.exomedia_notification_prev, "setVisibility", state.isPreviousEnabled() ? View.VISIBLE : View.INVISIBLE);
        bigContent.setInt(R.id.exomedia_notification_next, "setVisibility", state.isNextEnabled() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Creates a PendingIntent for the given action to the specified service
     *
     * @param action The action to use
     * @param serviceClass The service class to notify of intents
     * @return The resulting PendingIntent
     */
    private PendingIntent createPendingIntent(String action, Class<? extends Service> serviceClass) {
        Intent intent = new Intent(context, serviceClass);
        intent.setAction(action);

        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static class NotificationMediaState {
        private boolean isPlaying;
        private boolean isPreviousEnabled;
        private boolean isNextEnabled;

        public boolean isPlaying() {
            return isPlaying;
        }

        public boolean isPreviousEnabled() {
            return isPreviousEnabled;
        }

        public boolean isNextEnabled() {
            return isNextEnabled;
        }

        public void setPlaying(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }

        public void setPreviousEnabled(boolean isPreviousEnabled) {
            this.isPreviousEnabled = isPreviousEnabled;
        }

        public void setNextEnabled(boolean isNextEnabled) {
            this.isNextEnabled = isNextEnabled;
        }
    }
}
