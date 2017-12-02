package com.devbrackets.android.exomediademo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.playlistcore.helper.NotificationHelper;
import com.devbrackets.android.playlistcore.service.RemoteActions;

/**
 * A simple extension of the {@link NotificationHelper} class to handle the
 * Oreo notification channel registration. This is only required until
 * PlaylistCore 2.0 is released
 */
class DemoNotificationHelper extends NotificationHelper {
    private static final String CHANNEL_ID = "PlaylistCoreMediaNotificationChannel";

    public DemoNotificationHelper(@NonNull Context context) {
        super(context);
        buildNotificationChannel("Media Playback", "General playback of audio and video");
    }

    @Override
    public Notification getNotification(@Nullable PendingIntent pendingIntent, @NonNull Class<? extends Service> serviceClass) {
        setClickPendingIntent(pendingIntent);
        RemoteViews customNotificationViews = getCustomNotification(serviceClass);

        boolean allowSwipe = notificationInfo.getMediaState() == null || !notificationInfo.getMediaState().isPlaying();

        // NOTE: the only difference is the additional CHANNEL_ID parameter
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContent(customNotificationViews);
        builder.setContentIntent(pendingIntent);
        builder.setDeleteIntent(createPendingIntent(RemoteActions.ACTION_STOP, serviceClass));
        builder.setSmallIcon(notificationInfo.getAppIcon());
        builder.setAutoCancel(allowSwipe);
        builder.setOngoing(!allowSwipe);

        if (pendingIntent != null) {
            customNotificationViews.setOnClickPendingIntent(R.id.playlistcore_notification_touch_area, pendingIntent);
        }

        //Set the notification category on lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS);
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        //Build the notification and set the expanded content view if there is a service to inform of clicks
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && mediaServiceClass != null) {
            notification.bigContentView = getBigNotification(serviceClass);
            notification.bigContentView.setOnClickPendingIntent(R.id.playlistcore_big_notification_touch_area, pendingIntent);
        }

        return notification;
    }

    /**
     * Builds the notification channel using the specified name and description
     */
    private void buildNotificationChannel(CharSequence name, String description) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(description);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }
}
