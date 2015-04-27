package com.devbrackets.android.exomediademo.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;
import com.devbrackets.android.exomedia.service.EMPlaylistService;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.StartupActivity;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class AudioService extends EMPlaylistService<PlaylistManager.MediaItem, PlaylistManager> implements EMAudioFocusCallback {
    private static final int NOTIFICATION_ID = 1564; //Arbitrary
    private static final int FOREGROUND_REQUEST_CODE = 332; //Arbitrary
    private static final float AUDIO_DUCK_VOLUME = 0.1f;

    private Bitmap defaultLargeNotificationImage;
    private Bitmap largeNotificationImage;
    private Bitmap lockScreenArtwork;

    private NotificationTarget notificationImageTarget = new NotificationTarget();
    private LockScreenTarget lockScreenImageTarget = new LockScreenTarget();

    private Picasso picasso;

    @Override
    public void onCreate() {
        super.onCreate();

        picasso = Picasso.with(getApplicationContext());
    }

    @Override
    protected String getAppName() {
        return getResources().getString(R.string.app_name);
    }

    @Override
    protected int getNotificationId() {
        return NOTIFICATION_ID;
    }

    @Override
    protected float getAudioDuckVolume() {
        return AUDIO_DUCK_VOLUME;
    }

    @Override
    protected PlaylistManager getMediaPlaylistManager() {
        return App.getPlaylistManager();
    }

    @Override
    protected PendingIntent getNotificationClickPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), StartupActivity.class);
        return PendingIntent.getActivity(getApplicationContext(), FOREGROUND_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected Bitmap getDefaultLargeNotificationImage() {
        if (defaultLargeNotificationImage == null) {
            defaultLargeNotificationImage  = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }

        return defaultLargeNotificationImage;
    }

    @Override
    protected int getNotificationIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected int getLockScreenIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected void updateLargeNotificationImage(int size, PlaylistManager.MediaItem playlistItem) {
        picasso.load(playlistItem.getThumbnailUrl()).into(notificationImageTarget);
    }

    @Override
    protected void updateLockScreenArtwork(PlaylistManager.MediaItem playlistItem) {
        picasso.load(playlistItem.getArtworkUrl()).into(lockScreenImageTarget);
    }

    @Nullable
    @Override
    protected Bitmap getLockScreenArtwork() {
        return lockScreenArtwork;
    }

    @Nullable
    @Override
    protected Bitmap getLargeNotificationImage() {
        return largeNotificationImage;
    }

    /**
     * A class used to listen to the loading of the large notification images and perform
     * the correct functionality to update the notification once it is loaded.
     *
     * <b>NOTE:</b> This is a Picasso Image loader class
     */
    private class NotificationTarget implements Target {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            largeNotificationImage = bitmap;
            onLargeNotificationImageUpdated();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            largeNotificationImage = null;
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            //Purposefully left blank
        }
    }

    /**
     * A class used to listen to the loading of the large lock screen images and perform
     * the correct functionality to update the artwork once it is loaded.
     *
     * <b>NOTE:</b> This is a Picasso Image loader class
     */
    private class LockScreenTarget implements Target {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            lockScreenArtwork = bitmap;
            onLockScreenArtworkUpdated();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            lockScreenArtwork = null;
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            //Purposefully left blank
        }
    }
}