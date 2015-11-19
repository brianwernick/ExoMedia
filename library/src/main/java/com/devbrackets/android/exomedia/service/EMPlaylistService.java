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

package com.devbrackets.android.exomedia.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.EMLockScreen;
import com.devbrackets.android.exomedia.EMNotification;
import com.devbrackets.android.exomedia.EMRemoteActions;
import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMMediaStateEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;
import com.devbrackets.android.exomedia.listener.EMPlaylistServiceCallback;
import com.devbrackets.android.exomedia.listener.EMProgressCallback;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.util.EMAudioFocusHelper;
import com.devbrackets.android.exomedia.util.EMEventBus;

import java.util.LinkedList;
import java.util.List;

/**
 * A base service for adding media playback support using the {@link EMPlaylistManager}.
 * <p>
 * <b>NOTE:</b> This service will request a wifi wakelock if the item
 * being played isn't downloaded (see {@link #isDownloaded(EMPlaylistManager.PlaylistItem)}).
 * <p>
 * This requires the manifest permission &lt;uses-permission android:name="android.permission.WAKE_LOCK" /&gt;
 */
@SuppressWarnings("unused")
public abstract class EMPlaylistService<I extends EMPlaylistManager.PlaylistItem, M extends EMPlaylistManager<I>> extends Service implements EMAudioFocusCallback, EMProgressCallback {
    private static final String TAG = "EMPlaylistService";
    public static final String START_SERVICE = "EMPlaylistService.start";

    public enum MediaState {
        RETRIEVING,    // the MediaRetriever is retrieving music
        STOPPED,       // Stopped not preparing music
        PREPARING,     // Preparing - Buffering
        PLAYING,       // Active but could be paused due to loss of audio focus Needed for returning after we regain focus
        PAUSED,        // Paused but player ready.
        ERROR          // An audio error occurred, we are stopped
    }

    protected WifiManager.WifiLock wifiLock;
    protected EMAudioFocusHelper audioFocusHelper;

    protected EMAudioPlayer audioPlayer;
    protected EMMediaProgressEvent currentMediaProgress;

    protected EMNotification notificationHelper;
    protected EMLockScreen lockScreenHelper;

    protected boolean pausedForFocusLoss = false;
    protected MediaState currentState = MediaState.PREPARING;

    protected I currentPlaylistItem;
    protected int seekToPosition = -1;
    protected boolean immediatelyPause = false;

    protected AudioListener audioListener = new AudioListener();
    protected boolean pausedForSeek = false;
    protected boolean foregroundSetup;
    protected boolean notificationSetup;

    protected boolean onCreateCalled = false;
    protected Intent workaroundIntent = null;

    @Nullable
    protected String currentLargeNotificationUrl;
    @Nullable
    protected String currentLockScreenArtworkUrl;

    protected List<EMPlaylistServiceCallback> callbackList = new LinkedList<>();

    /**
     * Retrieves the ID to use for the notification and registering this
     * service as Foreground when media is playing. (Foreground is removed
     * when paused)
     *
     * @return The ID to use for the notification
     */
    protected abstract int getNotificationId();

    /**
     * Retrieves the volume level to use when audio focus has been temporarily
     * lost due to a higher importance notification.  The media will continue
     * playback but be reduced to the specified volume for the duration of the
     * notification.  This usually happens when receiving an alert for MMS, EMail,
     * etc.
     *
     * @return The volume level to use when a temporary notification sounds. [0.0 - 1.0]
     */
    protected abstract float getAudioDuckVolume();

    /**
     * Links the {@link EMPlaylistManager} that contains the information for playback
     * to this service.
     *
     * NOTE: this is only used for retrieving information, it isn't used to register notifications
     * for playlist changes, however as long as the change isn't breaking (e.g. cleared playlist)
     * then nothing additional needs to be performed.
     *
     * @return The {@link EMPlaylistManager} containing the playback information
     */
    protected abstract M getMediaPlaylistManager();

    /**
     * Returns the PendingIntent to use when the playback notification is clicked.
     * This is called when the playback is started initially to setup the notification
     * and the service as Foreground.
     *
     * @return The PendingIntent to use when the notification is clicked
     */
    protected abstract PendingIntent getNotificationClickPendingIntent();

    /**
     * Retrieves the Image to use for the large notification (the double tall notification)
     * when {@link #getLargeNotificationImage()} returns null.
     *
     * @return The image to use on the large notification when no other one is provided
     */
    protected abstract Bitmap getDefaultLargeNotificationImage();

    /**
     * Retrieves the Drawable resource that specifies the icon to place in the
     * status bar for the media playback notification.
     *
     * @return The Drawable resource id
     */
    @DrawableRes
    protected abstract int getNotificationIconRes();

    /**
     * Retrieves the Drawable resource that specifies the icon to place on the
     * lock screen to indicate the app the owns the content being displayed.
     *
     * @return The Drawable resource id
     */
    @DrawableRes
    protected abstract int getLockScreenIconRes();

    /**
     * Retrieves the bus that will be used for posting events.  This can be used in
     * conjunction with the {@link EMPlaylistServiceCallback} specified with {@link #registerCallback(EMPlaylistServiceCallback)}
     * or it can be used in place of the callbacks.
     *
     * @return The bus to post events to or null
     */
    @Nullable
    protected EMEventBus getBus() {
        return null;
    }

    /**
     * Retrieves the continuity bits associated with the service.  These
     * are the bits returned by {@link #onStartCommand(Intent, int, int)} and can be
     * one of the {@link #START_CONTINUATION_MASK} values.
     *
     * @return The continuity bits for the service [default: {@link #START_NOT_STICKY}]
     */
    public int getServiceFlag() {
        return START_NOT_STICKY;
    }

    /**
     * Used to determine if the device is connected to a network that has
     * internet access.  This is used in conjunction with {@link #isDownloaded(EMPlaylistManager.PlaylistItem)}
     * to determine what items in the playlist manager, specified with {@link #getMediaPlaylistManager()}, can be
     * played.
     *
     * @return True if the device currently has internet connectivity
     */
    protected boolean isNetworkAvailable() {
        return true;
    }

    /**
     * Used to determine if the specified playlistItem has been downloaded.  If this is true
     * then the downloaded copy will be used instead, and no network wakelock will be acquired.
     *
     * @param playlistItem The playlist item to determine if it is downloaded.
     * @return True if the specified playlistItem is downloaded. [default: false]
     */
    protected boolean isDownloaded(I playlistItem) {
        return false;
    }

    /**
     * Called when the media player has failed to play the current audio item.
     */
    protected void onMediaPlayerResetting() {
        //Purposefully left blank
    }

    /**
     * Called when the {@link #performStop()} has been called.
     *
     * @param playlistItem The playlist item that has been stopped
     */
    protected void onMediaStopped(I playlistItem) {
        //Purposefully left blank
    }

    /**
     * Called when a current audio item has ended playback.  This is called when we
     * are unable to play an audio item.
     *
     * @param playlistItem The PlaylistItem that has ended
     * @param currentPosition The position the playlist item ended at
     * @param duration The duration of the PlaylistItem
     */
    protected void onAudioPlaybackEnded(I playlistItem, long currentPosition, long duration) {
        //Purposefully left blank
    }

    /**
     * Called when an audio item has started playback.
     *
     * @param playlistItem The PlaylistItem that has started playback
     * @param currentPosition The position the playback has started at
     * @param duration The duration of the PlaylistItem
     */
    protected void onAudioPlaybackStarted(I playlistItem, long currentPosition, long duration) {
        //Purposefully left blank
    }

    /**
     * Called when the service is unable to seek to the next playable item when
     * no network is available.
     */
    protected void onNoNonNetworkItemsAvailable() {
        //Purposefully left blank
    }

    /**
     * Called when an audio item in playback has ended
     */
    protected void onAudioPlaybackEnded() {
        //Purposefully left blank
    }

    /**
     * Retrieves the image that will be displayed in the notification to represent
     * the currently playing item.
     *
     * @return The image to display in the notification or null
     */
    @Nullable
    protected Bitmap getLargeNotificationImage() {
        return null;
    }

    /**
     * Retrieves the image that will be displayed in the notification as a secondary
     * image.  This can be used to specify playback type (e.g. Chromecast).
     * <p>
     * This will be called any time the notification is updated
     *
     * @return The image to display in the secondary position
     */
    @Nullable
    protected Bitmap getLargeNotificationSecondaryImage() {
        return null;
    }

    /**
     * Retrieves the image that will be displayed in the notification as a secondary
     * image if {@link #getLargeNotificationSecondaryImage()} returns null.
     *
     * @return The fallback image to display in the secondary position
     */
    @Nullable
    protected Bitmap getDefaultLargeNotificationSecondaryImage() {
        return null;
    }

    /**
     * Called when the image in the notification needs to be updated.
     *
     * @param size The square size for the image to display
     * @param playlistItem The media item to get the image for
     */
    protected void updateLargeNotificationImage(int size, I playlistItem) {
        //Purposefully left blank
    }

    /**
     * Retrieves the image that will be displayed as the lock screen artwork
     * for the currently playing item.
     *
     * @return The image to display on the lock screen
     */
    @Nullable
    protected Bitmap getLockScreenArtwork() {
        return null;
    }

    /**
     * Called when the image for the Lock Screen needs to be updated.
     *
     * @param playlistItem The playlist item to get the lock screen image for
     */
    protected void updateLockScreenArtwork(I playlistItem) {
        //Purposefully left blank
    }

    /**
     * A generic method to determine if media is currently playing.  This is
     * used to determine the playback state for the notification.
     *
     * @return True if media is currently playing
     */
    protected boolean isPlaying() {
        if (currentItemIsAudio()) {
            return audioPlayer != null && audioPlayer.isPlaying();
        } else if (currentItemIsVideo()) {
            return getMediaPlaylistManager().getVideoView() != null && getMediaPlaylistManager().getVideoView().isPlaying();
        }

        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //Part of a workaround for some Samsung devices (see onStartCommand)
        if (onCreateCalled) {
            return;
        }

        onCreateCalled = true;
        super.onCreate();
        Log.d(TAG, "Service Created");

        onServiceCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service Destroyed");
        setMediaState(MediaState.STOPPED);

        relaxResources(true);
        getMediaPlaylistManager().unRegisterService();
        audioFocusHelper.setAudioFocusCallback(null);
        audioFocusHelper.abandonFocus();

        audioFocusHelper = null;
        notificationHelper = null;
        lockScreenHelper = null;

        onCreateCalled = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return getServiceFlag();
        }

        //This is a workaround for an issue on the Samsung Galaxy S3 (4.4.2) where the onStartCommand will occasionally get called before onCreate
        if (!onCreateCalled) {
            Log.d(TAG, "Starting Samsung workaround");
            workaroundIntent = intent;
            onCreate();
            return getServiceFlag();
        }

        if (EMRemoteActions.ACTION_START_SERVICE.equals(intent.getAction())) {
            startItemPlayback();

            seekToPosition = intent.getIntExtra(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, -1);
            immediatelyPause = intent.getBooleanExtra(EMRemoteActions.ACTION_EXTRA_START_PAUSED, false);
        } else {
            handleRemoteAction(intent.getAction(), intent.getExtras());
        }

        return getServiceFlag();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        onDestroy();
    }

    /**
     * Called when the Audio focus has been gained
     */
    @Override
    public boolean onAudioFocusGained() {
        if (!currentItemIsAudio()) {
            return false;
        }

        if (!audioPlayer.isPlaying() && pausedForFocusLoss) {
            audioPlayer.start();
            updateNotification();
        } else {
            audioPlayer.setVolume(1.0f, 1.0f); //reset the audio volume
        }

        return true;
    }

    /**
     * Called when the Audio focus has been lost
     */
    @Override
    public boolean onAudioFocusLost(boolean canDuckAudio) {
        if (!currentItemIsAudio()) {
            return false;
        }

        if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_NO_DUCK) {
            if (audioPlayer.isPlaying()) {
                pausedForFocusLoss = true;
                audioPlayer.pause();
                updateNotification();
            }
        } else {
            audioPlayer.setVolume(getAudioDuckVolume(), getAudioDuckVolume());
        }

        return true;
    }

    /**
     * Called when the media progress has been updated
     */
    @Override
    public boolean onProgressUpdated(EMMediaProgressEvent progressEvent) {
        currentMediaProgress = progressEvent;

        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onProgressUpdated(progressEvent)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a callback that will be informed of basic playback interactions
     * for use in UIs.
     *
     * @param callback The callback to be informed of playback updates
     */
    public void registerCallback(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.add(callback);
        }
    }

    /**
     * Removes a callback that was previously registered with {@link #registerCallback(EMPlaylistServiceCallback)}
     *
     * @param callback The callback to unRegister
     */
    public void unRegisterCallback(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.remove(callback);
        }
    }

    /**
     * Retrieves the current playback state of the service.
     *
     * @return The current playback state
     */
    public MediaState getCurrentMediaState() {
        return currentState;
    }

    /**
     * Retrieves the current playback progress.  If no
     * {@link com.devbrackets.android.exomedia.manager.EMPlaylistManager.PlaylistItem} has been played
     * then null will be returned.
     *
     * @return The current playback progress or null
     */
    @Nullable
    public EMMediaProgressEvent getCurrentMediaProgress() {
        return currentMediaProgress;
    }

    /**
     * Retrieves the current item change event which represents any media item changes.
     * This is intended as a utility method for initializing, or returning to, a media
     * playback UI.  In order to get the changed events you will need to register for
     * callbacks through {@link #registerCallback(EMPlaylistServiceCallback)} or through
     * Bus events by providing a bus with {@link #getBus()}
     *
     * @return The current PlaylistItem Changed event
     */
    public EMPlaylistItemChangedEvent<I> getCurrentItemChangedEvent() {
        boolean hasNext = getMediaPlaylistManager().isNextAvailable();
        boolean hasPrevious = getMediaPlaylistManager().isPreviousAvailable();

        return new EMPlaylistItemChangedEvent<>(currentPlaylistItem, hasPrevious, hasNext);
    }

    /**
     * Used to perform the onCreate functionality when the service is actually created.  This
     * should be overridden instead of {@link #onCreate()} due to a bug in some Samsung devices
     * where {@link #onStartCommand(Intent, int, int)} will get called before {@link #onCreate()}.
     */
    protected void onServiceCreate() {
        audioFocusHelper = new EMAudioFocusHelper(getApplicationContext());
        audioFocusHelper.setAudioFocusCallback(this);
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mcLock");

        notificationHelper = new EMNotification(getApplicationContext());
        lockScreenHelper = new EMLockScreen(getApplicationContext(), getClass());
        getMediaPlaylistManager().registerService(this);

        //Another part of the workaround for some Samsung devices
        if (workaroundIntent != null) {
            startService(workaroundIntent);
            workaroundIntent = null;
        }
    }

    /**
     * Performs the functionality to pause and/or resume
     * the media playback.  This is called through an intent
     * with the {@link EMRemoteActions#ACTION_PLAY_PAUSE}, through
     * {@link EMPlaylistManager#invokePausePlay()}
     */
    protected void performPlayPause() {
        if (isPlaying() || pausedForFocusLoss) {
            pausedForFocusLoss = false;
            performPause();
        } else {
            performPlay();
        }

        updateLockScreen();
        updateNotification();
    }

    /**
     * Performs the functionality to seek to the previous media
     * item.  This is called through an intent
     * with the {@link EMRemoteActions#ACTION_PREVIOUS}, through
     * {@link EMPlaylistManager#invokePrevious()}
     */
    protected void performPrevious() {
        seekToPosition = 0;
        immediatelyPause = !isPlaying();

        getMediaPlaylistManager().previous();
        startItemPlayback();
    }

    /**
     * Performs the functionality to seek to the next media
     * item.  This is called through an intent
     * with the {@link EMRemoteActions#ACTION_NEXT}, through
     * {@link EMPlaylistManager#invokeNext()}
     */
    protected void performNext() {
        seekToPosition = 0;
        immediatelyPause = !isPlaying();

        getMediaPlaylistManager().next();
        startItemPlayback();
    }

    /**
     * Performs the functionality to repeat the current
     * media item in playback.  This is called through an
     * intent with the {@link EMRemoteActions#ACTION_REPEAT},
     * through {@link EMPlaylistManager#invokeRepeat()}
     */
    protected void performRepeat() {
        //Left for the extending class to implement
    }

    /**
     * Performs the functionality to repeat the current
     * media item in playback.  This is called through an
     * intent with the {@link EMRemoteActions#ACTION_SHUFFLE},
     * through {@link EMPlaylistManager#invokeShuffle()}
     */
    protected void performShuffle() {
        //Left for the extending class to implement
    }

    /**
     * Performs the functionality for when a media item
     * has finished playback.  By default the completion
     * will seek to the next available media item.  This is
     * called from the Audio listener.
     */
    protected void performMediaCompletion() {
        //Left for the extending class to implement
    }

    /**
     * Performs the functionality to start a seek for the current
     * media item.  This is called through an intent
     * with the {@link EMRemoteActions#ACTION_SEEK_STARTED}, through
     * {@link EMPlaylistManager#invokeSeekStarted()}
     */
    protected void performSeekStarted() {
        EMVideoView videoView = getMediaPlaylistManager().getVideoView();
        boolean isPlaying = (currentItemIsAudio() && audioPlayer.isPlaying()) || (currentItemIsVideo() && videoView != null && videoView.isPlaying());

        if (isPlaying) {
            pausedForSeek = true;
            performPause();
        }
    }

    /**
     * Performs the functionality to end a seek for the current
     * media item.  This is called through an intent
     * with the {@link EMRemoteActions#ACTION_SEEK_ENDED}, through
     * {@link EMPlaylistManager#invokeSeekEnded(int)}
     */
    protected void performSeekEnded(int newPosition) {
        performSeek(newPosition);

        if (pausedForSeek) {
            performPlay();
            pausedForSeek = false;
        }
    }

    /**
     * Sets the media type to be allowed in the playlist.  If the type is changed
     * during media playback, the current item will be compared against the new
     * allowed type.  If the current item type and the new type are not compatible
     * then the playback will be seeked to the next valid item.
     *
     * @param newType The new allowed media type
     */
    protected void updateAllowedMediaType(EMPlaylistManager.MediaType newType) {
        //We seek through the items until an allowed one is reached, or none is reached and the service is stopped.
        if (newType != M.MediaType.AUDIO_AND_VIDEO && currentPlaylistItem != null && newType != currentPlaylistItem.getMediaType()) {
            performNext();
        }
    }

    /**
     * Informs the callbacks specified with {@link #registerCallback(EMPlaylistServiceCallback)}
     * and posts a Bus event if {@link #getBus()} has been specified, that the current playlist item
     * has changed.
     */
    protected void postPlaylistItemChanged() {
        boolean hasNext = getMediaPlaylistManager().isNextAvailable();
        boolean hasPrevious = getMediaPlaylistManager().isPreviousAvailable();

        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onPlaylistItemChanged(currentPlaylistItem, hasNext, hasPrevious)) {
                return;
            }
        }

        EMEventBus bus = getBus();
        if (bus != null) {
            bus.post(new EMPlaylistItemChangedEvent<>(currentPlaylistItem, hasPrevious, hasNext));
        }
    }

    /**
     * Informs the callbacks specified with {@link #registerCallback(EMPlaylistServiceCallback)}
     * and posts a Bus event if {@link #getBus()} has been specified, that the current media state
     * has changed.
     */
    protected void postMediaStateChanged() {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onMediaStateChanged(currentState)) {
                return;
            }
        }

        EMEventBus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaStateEvent(currentState));
        }
    }

    /**
     * Performs the functionality to stop the media playback.  This will perform any cleanup
     * and stop the service.
     */
    protected void performStop() {
        setMediaState(MediaState.STOPPED);
        if (currentPlaylistItem != null) {
            onMediaStopped(currentPlaylistItem);
        }

        // let go of all resources
        relaxResources(true);
        audioFocusHelper.abandonFocus();

        //Cleans out the avPlayListManager
        getMediaPlaylistManager().setParameters(null, 0);
        getMediaPlaylistManager().setPlaylistId(-1);

        stopSelf();
    }

    /**
     * Performs the functionality to seek the current media item
     * to the specified position.
     *
     * @param position The position to seek to in milliseconds
     */
    protected void performSeek(int position) {
        if (currentItemIsAudio()) {
            if (audioPlayer != null) {
                audioPlayer.seekTo(position);
            }
        } else if (currentItemIsVideo()) {
            EMVideoView videoView = getMediaPlaylistManager().getVideoView();
            if (videoView != null) {
                videoView.seekTo(position);
            }
        }
    }

    /**
     * Performs the functionality to actually pause the current media
     * playback.
     */
    protected void performPause() {
        if (currentItemIsAudio()) {
            if (audioPlayer != null) {
                audioPlayer.pause();
            }
        } else if (currentItemIsVideo()) {
            EMVideoView videoView = getMediaPlaylistManager().getVideoView();
            if (videoView != null) {
                videoView.pause();
            }
        }

        setMediaState(MediaState.PAUSED);
        stopForeground();
    }

    /**
     * Performs the functionality to actually play the current media
     * item.
     */
    protected void performPlay() {
        if (currentItemIsAudio()) {
            if (audioPlayer != null) {
                audioPlayer.start();
            }
        } else if (currentItemIsVideo()) {
            EMVideoView videoView = getMediaPlaylistManager().getVideoView();
            if (videoView != null) {
                videoView.start();
            }
        }

        setMediaState(MediaState.PLAYING);
        setupForeground();
    }

    /**
     * Determines if the current media item is an Audio item.  This is specified
     * with {@link EMPlaylistManager.PlaylistItem#getMediaType()}
     *
     * @return True if the current media item is an Audio item
     */
    protected boolean currentItemIsAudio() {
        return currentPlaylistItem != null && currentPlaylistItem.getMediaType() == EMPlaylistManager.MediaType.AUDIO;
    }

    /**
     * Determines if the current media item is a Video item.  This is specified
     * with {@link EMPlaylistManager.PlaylistItem#getMediaType()}
     *
     * @return True if the current media item is a video item
     */
    protected boolean currentItemIsVideo() {
        return currentPlaylistItem != null && currentPlaylistItem.getMediaType() == EMPlaylistManager.MediaType.VIDEO;
    }

    /**
     * Determines if the current media item is an other type.  This is specified
     * with {@link EMPlaylistManager.PlaylistItem#getMediaType()}
     *
     * @return True if the current media item is of the OTHER type
     */
    protected boolean currentItemIsOther() {
        return currentPlaylistItem != null && currentPlaylistItem.getMediaType() == EMPlaylistManager.MediaType.OTHER;
    }

    /**
     * This should be called when the extending class has loaded an updated
     * image for the Large Notification.
     */
    protected void onLargeNotificationImageUpdated() {
        updateNotification();
    }

    /**
     * This should be called when the extending class has loaded an updated
     * image for the LockScreen Artwork.
     */
    protected void onLockScreenArtworkUpdated() {
        updateLockScreen();
    }

    /**
     * Sets up the service as a Foreground service only if we aren't already registered as such
     */
    protected void setupForeground() {
        if (!foregroundSetup && notificationSetup) {
            foregroundSetup = true;
            startForeground(getNotificationId(), notificationHelper.getNotification(getNotificationClickPendingIntent()));
        }
    }

    /**
     * If the service is registered as a foreground service then it will be unregistered
     * as such without removing the notification
     */
    protected void stopForeground() {
        if (foregroundSetup) {
            foregroundSetup = false;
            stopForeground(false);
        }
    }

    /**
     * Starts the actual item playback, correctly determining if the
     * item is a video or an audio item.
     * <p>
     * <em><b>NOTE:</b></em> In order to play videos you will need to specify the
     * VideoView with {@link EMPlaylistManager#setVideoView(EMVideoView)}
     */
    protected void startItemPlayback() {
        if (currentItemIsAudio()) {
            onAudioPlaybackEnded();
        }

        seekToNextPlayableItem();
        mediaItemChanged();

        if (currentItemIsAudio()) {
            audioListener.resetRetryCount();
            playAudioItem();
        } else if (currentItemIsVideo()) {
            playVideoItem();
        } else if (currentItemIsOther()) {
            playOtherItem();
        } else if (getMediaPlaylistManager().isNextAvailable()) {
            //We get here if there was an error retrieving the currentPlaylistItem
            performNext();
        } else {
            //At this point there is nothing for us to play, so we stop the service
            performStop();
        }
    }

    /**
     * Starts the actual playback of the specified audio item
     */
    protected void playAudioItem() {
        stopVideoPlayback();
        initializeAudioPlayer();
        audioFocusHelper.requestFocus();

        boolean isItemDownloaded = isDownloaded(currentPlaylistItem);
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioPlayer.setDataSource(this, Uri.parse(isItemDownloaded ? currentPlaylistItem.getDownloadedMediaUri() : currentPlaylistItem.getMediaUrl()));

        setMediaState(MediaState.PREPARING);
        setupAsForeground();

        audioPlayer.prepareAsync();

        // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
        // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
        // we are NOT streaming, we want to release the lock.
        if (!isItemDownloaded) {
            wifiLock.acquire();
        } else if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Starts the actual playback of the specified video item.
     */
    protected void playVideoItem() {
        stopAudioPlayback();
        setupAsForeground();

        EMVideoView videoView = getMediaPlaylistManager().getVideoView();
        if (videoView != null) {
            videoView.stopPlayback();
            boolean isItemDownloaded = isDownloaded(currentPlaylistItem);
            videoView.setVideoURI(Uri.parse(isItemDownloaded ? currentPlaylistItem.getDownloadedMediaUri() : currentPlaylistItem.getMediaUrl()));
        }
    }

    /**
     * Starts the playback of the specified other item type.
     */
    protected void playOtherItem() {
        //Purposefully left blank
    }

    /**
     * Stops the AudioPlayer from playing.
     */
    protected void stopAudioPlayback() {
        audioFocusHelper.abandonFocus();

        if (audioPlayer != null) {
            audioPlayer.stopPlayback();
            audioPlayer.reset();
        }
    }

    /**
     * Stops the VideoView from playing if we have access to it.
     */
    protected void stopVideoPlayback() {
        EMVideoView videoView = getMediaPlaylistManager().getVideoView();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView.reset();
        }
    }

    /**
     * Reconfigures audioPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the audioPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * audioPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings.
     */
    protected void startAudioPlayer() {
        if (audioPlayer == null) {
            return;
        }

        if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck we have to pause, even if state is playing
            // Be we stay in the playing state so we know we have to resume playback once we get the focus back.
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                onAudioPlaybackEnded(currentPlaylistItem, audioPlayer.getCurrentPosition(), audioPlayer.getDuration());
            }

            return;
        } else if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_CAN_DUCK) {
            audioPlayer.setVolume(getAudioDuckVolume(), getAudioDuckVolume());
        } else {
            audioPlayer.setVolume(1.0f, 1.0f); // can be loud
        }

        if (!audioPlayer.isPlaying()) {
            audioPlayer.start();
            onAudioPlaybackStarted(currentPlaylistItem, audioPlayer.getCurrentPosition(), audioPlayer.getDuration());
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks, and the audioPlayer if requested
     *
     * @param releaseAudioPlayer True if the audioPlayer should be released
     */
    protected void relaxResources(boolean releaseAudioPlayer) {
        foregroundSetup = false;
        stopForeground(true);
        notificationHelper.dismiss();
        lockScreenHelper.release();

        if (releaseAudioPlayer) {
            if (audioPlayer != null) {
                audioPlayer.reset();
                audioPlayer.release();
                audioPlayer = null;
            }

            getMediaPlaylistManager().setCurrentIndex(Integer.MAX_VALUE);
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Updates the current MediaState and informs any listening classes.
     *
     * @param state The new MediaState
     */
    protected void setMediaState(MediaState state) {
        currentState = state;
        postMediaStateChanged();
    }

    /**
     * Iterates through the playList, starting with the current item, until we reach an item we can play.
     * Normally this will be the current item, however if they don't have network then
     * it will be the next downloaded item.
     */
    protected void seekToNextPlayableItem() {
        I currentItem = getMediaPlaylistManager().getCurrentItem();
        if (currentItem == null) {
            currentPlaylistItem = null;
            return;
        }

        //Only iterate through the list if we aren't connected to the internet
        if (!isNetworkAvailable()) {
            while (currentItem != null && !isDownloaded(currentItem)) {
                currentItem = getMediaPlaylistManager().next();
            }
        }

        //If we are unable to get a next playable item, post a network error
        if (currentItem == null) {
            onNoNonNetworkItemsAvailable();
        }

        currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
    }

    /**
     * Requests the service be transferred to the foreground, initializing the
     * LockScreen and Notification helpers for playback control.
     */
    protected void setupAsForeground() {
        //Sets up the Lock Screen playback controls
        lockScreenHelper.setLockScreenEnabled(true);
        lockScreenHelper.setLockScreenBaseInformation(getLockScreenIconRes());

        //Sets up the Notifications
        notificationHelper.setNotificationsEnabled(true);
        notificationHelper.setNotificationBaseInformation(getNotificationId(), getNotificationIconRes(), getClass());

        //Starts the service as the foreground audio player
        notificationSetup = true;
        setupForeground();

        updateLockScreen();
        updateNotification();
    }

    /**
     * Performs the process to update the playback controls and images in the notification
     * associated with the current playlist item.
     */
    protected void updateNotification() {
        if (currentPlaylistItem == null || !notificationSetup || notificationHelper == null) {
            return;
        }

        //Generate the notification state
        EMNotification.NotificationMediaState mediaState = new EMNotification.NotificationMediaState();
        mediaState.setNextEnabled(getMediaPlaylistManager().isNextAvailable());
        mediaState.setPreviousEnabled(getMediaPlaylistManager().isPreviousAvailable());
        mediaState.setPlaying(isPlaying());


        //Update the big notification images
        Bitmap bitmap = getLargeNotificationImage();
        if (bitmap == null) {
            bitmap = getDefaultLargeNotificationImage();
        }

        Bitmap secondaryImage = getLargeNotificationSecondaryImage();
        if (secondaryImage == null) {
            secondaryImage = getDefaultLargeNotificationSecondaryImage();
        }

        //Finish up the update
        String title = currentPlaylistItem.getTitle();
        String album = currentPlaylistItem.getAlbum();
        String artist = currentPlaylistItem.getArtist();
        notificationHelper.updateNotificationInformation(title, album, artist, bitmap, secondaryImage, mediaState);
    }

    /**
     * Performs the process to update the playback controls and the background
     * (artwork) image displayed on the lock screen.
     */
    protected void updateLockScreen() {
        if (currentPlaylistItem == null || !notificationSetup || lockScreenHelper == null) {
            return;
        }

        //Generate the notification state
        EMNotification.NotificationMediaState mediaState = new EMNotification.NotificationMediaState();
        mediaState.setNextEnabled(getMediaPlaylistManager().isNextAvailable());
        mediaState.setPreviousEnabled(getMediaPlaylistManager().isPreviousAvailable());
        mediaState.setPlaying(isPlaying());


        //Finish up the update
        String title = currentPlaylistItem.getTitle();
        String album = currentPlaylistItem.getAlbum();
        String artist = currentPlaylistItem.getArtist();
        lockScreenHelper.updateLockScreenInformation(title, album, artist, getLockScreenArtwork(), mediaState);
    }

    /**
     * Called when the current media item has changed, this will update the notification and
     * lock screen values.
     */
    protected void mediaItemChanged() {
        //Validates that the currentPlaylistItem is for the currentItem
        if (!getMediaPlaylistManager().isPlayingItem(currentPlaylistItem)) {
            Log.d(TAG, "forcing currentPlaylistItem update");
            currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
        }

        //Starts the notification loading
        if (currentPlaylistItem != null && (currentLargeNotificationUrl == null || !currentLargeNotificationUrl.equals(currentPlaylistItem.getThumbnailUrl()))) {
            int size = getResources().getDimensionPixelSize(R.dimen.exomedia_big_notification_height);
            updateLargeNotificationImage(size, currentPlaylistItem);
            currentLargeNotificationUrl = currentPlaylistItem.getThumbnailUrl();
        }

        //Starts the lock screen loading
        if (currentPlaylistItem != null && (currentLockScreenArtworkUrl == null || !currentLockScreenArtworkUrl.equalsIgnoreCase(currentPlaylistItem.getArtworkUrl()))) {
            updateLockScreenArtwork(currentPlaylistItem);
            currentLockScreenArtworkUrl = currentPlaylistItem.getArtworkUrl();
        }

        postPlaylistItemChanged();
    }

    /**
     * Handles the remote actions from the big notification and lock screen to control
     * the audio playback
     *
     * @param action The action from the intent to handle
     * @param extras The extras packaged with the intent associated with the action
     */
    protected void handleRemoteAction(String action, Bundle extras) {
        if (action == null || action.isEmpty()) {
            return;
        }

        switch (action) {
            case EMRemoteActions.ACTION_PLAY_PAUSE:
                performPlayPause();
                break;

            case EMRemoteActions.ACTION_NEXT:
                performNext();
                break;

            case EMRemoteActions.ACTION_PREVIOUS:
                performPrevious();
                break;

            case EMRemoteActions.ACTION_REPEAT:
                performRepeat();
                break;

            case EMRemoteActions.ACTION_SHUFFLE:
                performShuffle();
                break;

            case EMRemoteActions.ACTION_STOP:
                performStop();
                break;

            case EMRemoteActions.ACTION_SEEK_STARTED:
                performSeekStarted();
                break;

            case EMRemoteActions.ACTION_SEEK_ENDED:
                performSeekEnded(extras.getInt(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, 0));
                break;

            case EMRemoteActions.ACTION_ALLOWED_TYPE_CHANGED:
                updateAllowedMediaType((EMPlaylistManager.MediaType) extras.getSerializable(EMRemoteActions.ACTION_EXTRA_ALLOWED_TYPE));
                break;

            default:
                break;
        }
    }

    /**
     * Initializes the audio player.
     * If the audio player has already been initialized, then it will
     * be reset to prepare for the next playback item.
     */
    protected void initializeAudioPlayer() {
        if (audioPlayer != null) {
            audioPlayer.reset();
            return;
        }

        audioPlayer = new EMAudioPlayer(getApplicationContext());
        audioPlayer.setBus(getBus());
        audioPlayer.startProgressPoll(this);
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        //Sets the listeners
        audioPlayer.setOnPreparedListener(audioListener);
        audioPlayer.setOnCompletionListener(audioListener);
        audioPlayer.setOnErrorListener(audioListener);
    }

    /**
     * A class to listen to the EMAudioPlayer events, and will
     * retry audio playback once when an error is encountered.
     * This is done to workaround an issue on older (pre 4.1)
     * devices where playback will fail due to a race condition
     * in the {@link MediaPlayer}
     */
    private class AudioListener implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        private static final int MAX_RETRY_COUNT = 1;
        private int retryCount = 0;

        @Override
        public void onCompletion(MediaPlayer mp) {
            //Make sure to only perform this functionality when playing audio
            if (currentItemIsAudio()) {
                performMediaCompletion();
            }
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            //Make sure to only perform this functionality when playing audio
            if (!currentItemIsAudio()) {
                return false;
            }

            //The retry count is a workaround for when the EMAudioPlayer will occasionally fail to load valid content due to the MediaPlayer on pre 4.1 devices
            if (++retryCount <= MAX_RETRY_COUNT) {
                Log.d(TAG, "Retrying audio playback.  Retry count: " + retryCount);
                playAudioItem();
                return false;
            }

            onMediaPlayerResetting();
            Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);

            setMediaState(MediaState.ERROR);
            relaxResources(true);
            audioFocusHelper.abandonFocus();
            return false;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            //Make sure to only perform this functionality when playing audio
            if (!currentItemIsAudio()) {
                return;
            }

            retryCount = 0;
            setMediaState(MediaState.PLAYING);
            startAudioPlayer();

            //Immediately pauses
            if (immediatelyPause) {
                immediatelyPause = false;
                if (audioPlayer.isPlaying()) {
                    performPause();
                }
            }

            //Seek to the correct position
            if (seekToPosition > 0) {
                performSeek(seekToPosition);
                seekToPosition = -1;
            }

            updateLockScreen();
            updateNotification();
        }

        public void resetRetryCount() {
            retryCount = 0;
        }
    }
}