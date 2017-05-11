package com.devbrackets.android.exomediademo.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.exomediademo.playlist.AudioApi;
import com.devbrackets.android.exomediademo.playlist.VideoApi;
import com.devbrackets.android.exomediademo.ui.activity.StartupActivity;
import com.devbrackets.android.playlistcore.api.AudioPlayerApi;
import com.devbrackets.android.playlistcore.service.BasePlaylistService;
/**
 * A simple service that extends {@link BasePlaylistService} in order to provide
 * the application specific information required.
 */
public class MediaService extends BasePlaylistService<MediaItem, PlaylistManager> {
    private static final int NOTIFICATION_ID = 1564; //Arbitrary
    private static final int FOREGROUND_REQUEST_CODE = 332; //Arbitrary
    private static final float AUDIO_DUCK_VOLUME = 0.1f;

    private Bitmap defaultLargeNotificationImage;
    private Bitmap largeNotificationImage;
    private Bitmap lockScreenArtwork;

    private NotificationTarget notificationImageTarget = new NotificationTarget();
    private LockScreenTarget lockScreenImageTarget = new LockScreenTarget();

    private RequestManager glide;

    @Override
    public void onCreate() {
        super.onCreate();
        glide = Glide.with(getApplicationContext());
    }

    @Override
    protected void performOnMediaCompletion() {
        performNext();
        immediatelyPause = false;
    }

    @NonNull
    @Override
    protected AudioPlayerApi getNewAudioPlayer() {
        return new AudioApi(new AudioPlayer(getApplicationContext()));
    }

    @Override
    protected int getNotificationId() {
        return NOTIFICATION_ID;
    }

    @Override
    protected float getAudioDuckVolume() {
        return AUDIO_DUCK_VOLUME;
    }

    @NonNull
    @Override
    protected PlaylistManager getPlaylistManager() {
        return App.getPlaylistManager();
    }

    @NonNull
    @Override
    protected PendingIntent getNotificationClickPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), StartupActivity.class);
        return PendingIntent.getActivity(getApplicationContext(), FOREGROUND_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected Bitmap getDefaultLargeNotificationImage() {
        if (defaultLargeNotificationImage == null) {
            defaultLargeNotificationImage = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }

        return defaultLargeNotificationImage;
    }

    @Nullable
    @Override
    protected Bitmap getDefaultLargeNotificationSecondaryImage() {
        return null;
    }

    @Override
    protected int getNotificationIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected int getRemoteViewIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected void updateLargeNotificationImage(int size, MediaItem playlistItem) {
        glide.load(playlistItem.getThumbnailUrl()).asBitmap().into(notificationImageTarget);
    }

    @Override
    protected void updateRemoteViewArtwork(MediaItem playlistItem) {
        glide.load(playlistItem.getArtworkUrl()).asBitmap().into(lockScreenImageTarget);
    }

    @Nullable
    @Override
    protected Bitmap getRemoteViewArtwork() {
        return lockScreenArtwork;
    }

    @Nullable
    @Override
    protected Bitmap getLargeNotificationImage() {
        return largeNotificationImage;
    }

    /**
     * Overridden to allow updating the Title, SubTitle, and description in
     * the VideoView (VideoControls)
     */
    @Override
    protected boolean playVideoItem() {
        if (super.playVideoItem()) {
            updateVideoControls();
            return true;
        }

        return false;
    }

    /**
     * Helper method used to verify we can access the {@link VideoView#getVideoControls()}
     * to update both the text and available next/previous buttons
     */
    private void updateVideoControls() {
        VideoApi videoApi = (VideoApi) getPlaylistManager().getVideoPlayer();
        if (videoApi == null) {
            return;
        }

        VideoView videoView = videoApi.getVideoView();
        if (videoView == null) {
            return;
        }

        VideoControls videoControls = videoView.getVideoControls();
        if (videoControls != null) {
            updateVideoControlsText(videoControls);
            updateVideoControlsButtons(videoControls);
        }
    }

    private void updateVideoControlsText(@NonNull VideoControls videoControls) {
        if (currentPlaylistItem != null) {
            videoControls.setTitle(currentPlaylistItem.getTitle());
            videoControls.setSubTitle(currentPlaylistItem.getAlbum());
            videoControls.setDescription(currentPlaylistItem.getArtist());
        }
    }

    private void updateVideoControlsButtons(@NonNull VideoControls videoControls) {
        videoControls.setPreviousButtonEnabled(getPlaylistManager().isPreviousAvailable());
        videoControls.setNextButtonEnabled(getPlaylistManager().isNextAvailable());
    }

    /**
     * A class used to listen to the loading of the large notification images and perform
     * the correct functionality to update the notification once it is loaded.
     * <p>
     * <b>NOTE:</b> This is a Glide Image loader class
     */
    private class NotificationTarget extends SimpleTarget<Bitmap> {
        @Override
        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            largeNotificationImage = resource;
            onLargeNotificationImageUpdated();
        }
    }

    /**
     * A class used to listen to the loading of the large lock screen images and perform
     * the correct functionality to update the artwork once it is loaded.
     * <p>
     * <b>NOTE:</b> This is a Glide Image loader class
     */
    private class LockScreenTarget extends SimpleTarget<Bitmap> {
        @Override
        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            lockScreenArtwork = resource;
            onRemoteViewArtworkUpdated();
        }
    }
}
