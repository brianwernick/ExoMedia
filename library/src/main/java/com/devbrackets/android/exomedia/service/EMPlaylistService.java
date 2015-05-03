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
import android.os.Build;
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
import com.devbrackets.android.exomedia.event.EMMediaAllowedTypeChangedEvent;
import com.devbrackets.android.exomedia.event.EMMediaNextEvent;
import com.devbrackets.android.exomedia.event.EMMediaPlayPauseEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreviousEvent;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMMediaSeekEndedEvent;
import com.devbrackets.android.exomedia.event.EMMediaSeekStartedEvent;
import com.devbrackets.android.exomedia.event.EMMediaStateEvent;
import com.devbrackets.android.exomedia.event.EMMediaStopEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;
import com.devbrackets.android.exomedia.listener.EMPlaylistServiceCallback;
import com.devbrackets.android.exomedia.listener.EMProgressCallback;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.util.EMAudioFocusHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO: add comments
 *
 * <b>NOTE:</b> This service will request a wifi wakelock if the item
 * being played isn't downloaded (see {@link #isDownloaded(EMPlaylistManager.PlaylistItem)}).
 * </p>
 * This requires the manifest permission &lt;uses-permission android:name="android.permission.WAKE_LOCK" /&gt;
 */
@SuppressWarnings("unused")
public abstract class EMPlaylistService<I extends EMPlaylistManager.PlaylistItem, M extends EMPlaylistManager<I>> extends Service implements
        EMAudioFocusCallback, EMProgressCallback {
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

    private boolean pausedForFocusLoss = false;
    protected MediaState currentState = MediaState.PREPARING;

    protected I currentPlaylistItem;
    protected M.MediaType currentMediaType = M.MediaType.NONE;
    protected int seekToPosition = -1;
    protected boolean immediatelyPause = false;

    protected AudioListener audioListener = new AudioListener();
    protected boolean pausedForSeek = false;
    protected boolean foregroundSetup;

    protected boolean onCreateCalled = false;
    protected Intent workaroundIntent = null;

    protected EventSubscriptionProvider subscriptionProvider;
    protected List<EMPlaylistServiceCallback> callbackList = new LinkedList<>();

    protected abstract String getAppName();
    protected abstract int getNotificationId();
    protected abstract float getAudioDuckVolume();
    protected abstract M getMediaPlaylistManager();
    protected abstract PendingIntent getNotificationClickPendingIntent();
    protected abstract Bitmap getDefaultLargeNotificationImage();

    @DrawableRes
    protected abstract int getNotificationIconRes();

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
    protected Bus getBus() {
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

    protected void onMediaPlayerResetting() {
        //Purposefully left blank
    }

    protected void onMediaStopped(I playlistItem){
        //Purposefully left blank
    }

    protected void onAudioPlaybackEnded(I playlistItem, long currentPosition, long duration) {
        //Purposefully left blank
    }

    protected void onAudioPlaybackStarted(I playlistItem, long currentPosition, long duration) {
        //Purposefully left blank
    }

    protected void onNoNonNetworkItemsAvailable() {
        //Purposefully left blank
    }

    protected void onAudioPlaybackEnded() {
        //Purposefully left blank
    }

    @Nullable
    protected Bitmap getLargeNotificationImage() {
        return null;
    }

    protected void updateLargeNotificationImage(int size, I playlistItem) {
        //Purposefully left blank
    }

    @Nullable
    protected Bitmap getLockScreenArtwork() {
        return null;
    }

    protected void updateLockScreenArtwork(I playlistItem) {
        //Purposefully left blank
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

        audioFocusHelper = new EMAudioFocusHelper(getApplicationContext());
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mcLock");

        notificationHelper = new EMNotification(getApplicationContext());
        lockScreenHelper = new EMLockScreen(getApplicationContext(), getClass());

        //Another part of the workaround for some Samsung devices
        if (workaroundIntent != null) {
            startService(workaroundIntent);
            workaroundIntent = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service Destroyed");
        setMediaState(MediaState.STOPPED);

        relaxResources(true);
        audioFocusHelper.abandonFocus();
        lockScreenHelper.release();

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
        //onTaskRemoved was added in API 14 (ICS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onTaskRemoved(rootIntent);
        }

        onDestroy();
    }

    @Override
    public boolean onAudioFocusGained() {
        if (!audioPlayer.isPlaying() && pausedForFocusLoss) {
            audioPlayer.start();
        } else {
            audioPlayer.setVolume(1.0f, 1.0f); //reset the audio volume
        }

        return true;
    }

    @Override
    public boolean onAudioFocusLost(boolean canDuckAudio) {
        if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_NO_DUCK) {
            if (audioPlayer.isPlaying()) {
                pausedForFocusLoss = true;
                audioPlayer.pause();
            }
        } else {
            audioPlayer.setVolume(getAudioDuckVolume(), getAudioDuckVolume());
        }

        return true;
    }

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

    public void registerCallback(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.add(callback);
        }
    }

    public void unRegisterCallback(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.remove(callback);
        }
    }

    public MediaState getCurrentMediaState() {
        return currentState;
    }

    @Nullable
    public EMMediaProgressEvent getCurrentMediaProgress() {
        return currentMediaProgress;
    }

    public EMPlaylistItemChangedEvent<I> getCurrentItemChangedEvent() {
        boolean hasNext = getMediaPlaylistManager().isNextAvailable();
        boolean hasPrevious = getMediaPlaylistManager().isPreviousAvailable();

        return new EMPlaylistItemChangedEvent<>(currentPlaylistItem, currentMediaType, hasPrevious, hasNext);
    }

    /**
     * Registers an internal {@link com.devbrackets.android.exomedia.service.EMPlaylistService.EventSubscriptionProvider}
     * that will listen to Bus Events and Provide Bus Event related to the {@link EMPlaylistService}.  The bus will NOT
     *  be used for posting events, in order to enable that a bus needs to be provided with {@link #getBus()}
     *
     * @param bus The bus to register
     */
    protected void registerBus(Bus bus) {
        if (subscriptionProvider == null) {
            subscriptionProvider = new EventSubscriptionProvider<>(this);
        }

        if (bus != null) {
            subscriptionProvider.registerBus(bus);
        }
    }

    /**
     * UnRegisters a previously registered bus for the
     * Events and Provides. (see {@link #registerBus(Bus)}
     */
    protected void unRegisterBus() {
        if (subscriptionProvider != null) {
            subscriptionProvider.unRegisterBus();
        }
    }

    protected void performPlayPause() {
        if (currentItemIsAudio()) {
            if (audioPlayer.isPlaying() || pausedForFocusLoss) {
                pausedForFocusLoss = false;
                performPause();
            } else {
                performPlay();
            }

            updateNotification();
            updateLockScreen();
        }
    }

    protected void performPrevious() {
        getMediaPlaylistManager().previous();
        startItemPlayback();
        seekToPosition = 0;
    }

    protected void performNext() {
        getMediaPlaylistManager().next();
        startItemPlayback();
        seekToPosition = 0;
    }

    protected void performMediaCompletion() {
        performNext();
    }

    protected void performSeekStarted() {
        if (currentItemIsAudio() && audioPlayer.isPlaying()) {
            pausedForSeek = true;
            audioPlayer.pause();
        }
    }

    protected void performSeekEnded(int newPosition) {
        if (currentItemIsAudio()) {
            audioPlayer.seekTo(newPosition);

            if (pausedForSeek) {
                audioPlayer.start();
                pausedForSeek = false;
            }
        }
    }

    protected void updateAllowedMediaType(EMPlaylistManager.MediaType newType) {
        //We seek through the items until an allowed one is reached, or none is reached and the service is stopped.
        if (newType != M.MediaType.AUDIO_AND_VIDEO && newType != currentMediaType) {
            performNext();
        }
    }

    protected void postPlaylistItemChanged() {
        boolean hasNext = getMediaPlaylistManager().isNextAvailable();
        boolean hasPrevious = getMediaPlaylistManager().isPreviousAvailable();

        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onPlaylistItemChanged(currentPlaylistItem, currentMediaType, hasNext, hasPrevious)) {
                return;
            }
        }

        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMPlaylistItemChangedEvent<>(currentPlaylistItem, currentMediaType, hasPrevious, hasNext));
        }
    }

    protected void postMediaStateChanged() {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onMediaStateChanged(currentState)) {
                return;
            }
        }

        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaStateEvent(currentState));
        }
    }

    protected void performStop(boolean force) {
        if (currentState != MediaState.PLAYING && currentState != MediaState.PAUSED && !force) {
            return;
        }

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

    protected void performSeek(int position) {
        if (currentItemIsAudio() && (currentState == MediaState.PLAYING || currentState == MediaState.PAUSED)) {
            audioPlayer.seekTo(position);
        }
    }

    protected void performPause() {
        if (currentItemIsAudio()) {
            audioPlayer.pause();
            setMediaState(MediaState.PAUSED);
        }
    }

    protected void performPlay() {
        if (currentItemIsAudio()) {
            audioPlayer.start();
            setMediaState(MediaState.PLAYING);
        }
    }

    protected boolean currentItemIsAudio() {
        return currentPlaylistItem != null && currentPlaylistItem.isAudio();
    }

    protected boolean currentItemIsVideo() {
        return currentPlaylistItem != null && currentPlaylistItem.isVideo();
    }

    protected void onLargeNotificationImageUpdated() {
        updateNotification();
    }

    protected void onLockScreenArtworkUpdated() {
        updateLockScreen();
    }

    /**
     * Handles the remote actions from the big notification and lock screen to control
     * the audio playback
     *
     * @param action The intents action
     * @param extras The intent extras
     */
    private void handleRemoteAction(String action, Bundle extras) {
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

            case EMRemoteActions.ACTION_STOP:
                performStop(false);
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

    private void initializeAudioPlayer() {
        if (audioPlayer != null) {
            audioPlayer.reset();
            return;
        }

        audioPlayer = new EMAudioPlayer(getApplicationContext());
        audioPlayer.startProgressPoll(this);
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        //Sets the listeners
        audioPlayer.setOnPreparedListener(audioListener);
        audioPlayer.setOnCompletionListener(audioListener);
        audioPlayer.setOnErrorListener(audioListener);
    }

    private void startItemPlayback() {
        relaxResources(false);

        if (currentPlaylistItem != null && currentPlaylistItem.isAudio()) {
            onAudioPlaybackEnded();
        }

        I currentItem = currentPlaylistItem;
        seekToNextPlayableItem();

        mediaItemChanged(currentItem);

        if (currentItemIsAudio()) {
            audioListener.resetRetryCount();
            playAudioItem();
        } else if (currentItemIsVideo()) {
            playVideoItem();
        } else if (getMediaPlaylistManager().isNextAvailable()) {
            //We get here if there was an error retrieving the currentPlaylistItem
            performNext();
        } else {
            //At this point there is nothing for us to play, so we stop the service
            performStop(true);
        }
    }

    private void playAudioItem() {
        stopVideoPlayback();
        initializeAudioPlayer();
        audioFocusHelper.requestFocus();

        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioPlayer.setDataSource(this, Uri.parse(currentPlaylistItem.getMediaUrl()));

        setMediaState(MediaState.PREPARING);
        setupAsForeground();

        audioPlayer.prepareAsync();

        // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
        // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
        // we are NOT streaming, we want to release the lock.
        if (!isDownloaded(currentPlaylistItem)) {
            wifiLock.acquire();
        } else if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void playVideoItem() {
        stopAudioPlayback();
        setupAsForeground();
    }

    /**
     * Stops the AudioPlayer from playing.
     */
    private void stopAudioPlayback() {
        audioFocusHelper.abandonFocus();

        if (audioPlayer != null) {
            audioPlayer.stopPlayback();
            audioPlayer.reset();
        }
    }

    /**
     * Stops the VideoView from playing if we have access to it.
     */
    private void stopVideoPlayback() {
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
    private void startAudioPlayer() {
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
    private void relaxResources(boolean releaseAudioPlayer) {
        foregroundSetup = false;
        stopForeground(true);

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
    private void setMediaState(MediaState state) {
        currentState = state;
        postMediaStateChanged();
    }

    /**
     * Iterates through the playList, starting with the current item, until we reach an item we can play.
     * Normally this will be the current item, however if they don't have network then
     * it will be the next downloaded item.
     */
    private void seekToNextPlayableItem() {
        I currentItem = getMediaPlaylistManager().getCurrentItem();
        if (currentItem == null) {
            currentPlaylistItem = null;
            return;
        }

        //Only iterate through the list if we aren't connected to the internet
        if(!isNetworkAvailable()) {
            while(currentItem != null && !isDownloaded(currentItem)) {
                currentItem = getMediaPlaylistManager().next();
            }
        }

        //If we are unable to get a next playable item, post a network error
        if(currentItem == null) {
            onNoNonNetworkItemsAvailable();
        }

        currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
    }

    /**
     * Requests the service be transferred to the foreground, initializing the
     * LockScreen and Notification helpers for playback control.
     */
    private void setupAsForeground() {
        //Sets up the Lock Screen playback controls
        lockScreenHelper.setLockScreenEnabled(true);
        lockScreenHelper.setLockScreenBaseInformation(getLockScreenIconRes());

        //Sets up the Notifications
        notificationHelper.setNotificationsEnabled(true);
        notificationHelper.setNotificationBaseInformation(getNotificationId(), getNotificationIconRes(), getClass());

        //Starts the service as the foreground audio player
        startForeground(getNotificationId(), notificationHelper.getNotification(getNotificationClickPendingIntent()));
        foregroundSetup = true;

        updateLockScreen();
        updateNotification();
    }

    /**
     * Performs the process to update the playback controls and images in the notification
     * associated with the current playlist item.
     */
    private void updateNotification() {
        if (currentPlaylistItem == null || audioPlayer == null || !foregroundSetup) {
            return;
        }

        String title = currentPlaylistItem.getTitle();

        EMNotification.NotificationMediaState mediaState = null;

        //We only want the expanded notification for audio
        if (currentItemIsAudio()) {
            mediaState = new EMNotification.NotificationMediaState();
            mediaState.setNextEnabled(getMediaPlaylistManager().isNextAvailable());
            mediaState.setPreviousEnabled(getMediaPlaylistManager().isPreviousAvailable());
            mediaState.setPlaying(audioPlayer.isPlaying());
        }

        Bitmap bitmap = getLargeNotificationImage();
        if (bitmap == null) {
            bitmap = getDefaultLargeNotificationImage();
        }

        notificationHelper.updateNotificationInformation(getAppName(), title, bitmap, mediaState);
    }

    /**
     * Performs the process to update the playback controls and the background
     * (artwork) image displayed on the lock screen.
     */
    private void updateLockScreen() {
        if (currentPlaylistItem == null || audioPlayer == null || !foregroundSetup) {
            return;
        }

        String title = getAppName();
        String subTitle = currentPlaylistItem.getTitle();


        EMNotification.NotificationMediaState mediaState = new EMNotification.NotificationMediaState();
        mediaState.setNextEnabled(getMediaPlaylistManager().isNextAvailable());
        mediaState.setPreviousEnabled(getMediaPlaylistManager().isPreviousAvailable());
        mediaState.setPlaying(audioPlayer.isPlaying());

        lockScreenHelper.updateLockScreenInformation(title, subTitle, getLockScreenArtwork(), mediaState);
    }

    private void mediaItemChanged(I currentItem) {
        currentMediaType = getMediaPlaylistManager().getCurrentItemType();

        //Validates that the currentPlaylistItem is for the currentItem
        if (!getMediaPlaylistManager().isPlayingItem(currentPlaylistItem)) {
            Log.d(TAG, "forcing currentPlaylistItem update");
            currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
        }

        //Starts the notification loading
        if (currentPlaylistItem != null && (currentItem == null || !currentItem.getThumbnailUrl().equals(currentPlaylistItem.getThumbnailUrl()))) {
            int size = getResources().getDimensionPixelSize(R.dimen.exomedia_big_notification_height);
            updateLargeNotificationImage(size, currentPlaylistItem);
        }

        //Starts the lock screen loading
        if (currentPlaylistItem != null && (currentItem == null || !currentItem.getArtworkUrl().equalsIgnoreCase(currentPlaylistItem.getArtworkUrl()))) {
            updateLockScreenArtwork(currentPlaylistItem);
        }

        postPlaylistItemChanged();
    }

    /**
     * A class to listen to the EMAudioPlayer events
     */
    private class AudioListener implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        private static final int MAX_RETRY_COUNT = 1;
        private int retryCount = 0;

        @Override
        public void onCompletion(MediaPlayer mp) {
            performMediaCompletion();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
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

            updateNotification();
        }

        public void resetRetryCount() {
            retryCount = 0;
        }
    }

    /**
     * A container that allows us to easily register the appropriate subscribe and produce
     * methods for the {@link EMPlaylistService}.
     *
     * @param <T> The {@link EMPlaylistService} to subscribe with
     */
    private static class EventSubscriptionProvider<T extends EMPlaylistService> {
        private T playlistService;
        private Bus bus;
        private boolean isRegistered;

        public EventSubscriptionProvider(T playlistService) {
            this.playlistService = playlistService;
        }

        public void registerBus(Bus bus) {
            if (isRegistered) {
                return;
            }

            this.bus = bus;
            bus.register(this);
        }

        public void unRegisterBus() {
            if (isRegistered) {
                bus.unregister(this);
            }

            isRegistered = false;
            bus = null;
        }

        public boolean isRegistered() {
            return isRegistered;
        }

        @Subscribe
        public void onPlayPauseClickEvent(EMMediaPlayPauseEvent event) {
            playlistService.performPlayPause();
        }

        @Subscribe
        public void onStopEvent(EMMediaStopEvent event) {
            playlistService.performStop(false);
        }

        @Subscribe
        public void onPreviousButtonClickEvent(EMMediaPreviousEvent event) {
            playlistService.performPrevious();
        }

        @Subscribe
        public void onNextButtonClickEvent(EMMediaNextEvent event) {
            playlistService.performNext();
        }

        @Subscribe
        public void onSeekStartedEvent(EMMediaSeekStartedEvent event) {
            playlistService.performSeekStarted();
        }

        @Subscribe
        public void onSeekEndedEvent(EMMediaSeekEndedEvent event) {
            playlistService.performSeekEnded((int) event.getSeekPosition());
        }

        @Subscribe
        public void onAllowedMediaTypeChangeEvent(EMMediaAllowedTypeChangedEvent event) {
            playlistService.updateAllowedMediaType(event.allowedType);
        }

        @Produce
        public EMPlaylistItemChangedEvent produceEMPlaylistItemChangedEvent() {
            return playlistService.getCurrentItemChangedEvent();
        }

        @Produce
        public EMMediaStateEvent produceMediaStateEvent() {
            return new EMMediaStateEvent(playlistService.getCurrentMediaState());
        }

        @Produce
        public EMMediaProgressEvent produceMediaProgressEvent() {
            return playlistService.getCurrentMediaProgress();
        }
    }
}