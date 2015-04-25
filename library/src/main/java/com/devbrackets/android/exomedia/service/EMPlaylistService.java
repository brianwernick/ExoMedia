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
import com.devbrackets.android.exomedia.event.EMAllowedMediaTypeChangedEvent;
import com.devbrackets.android.exomedia.event.EMAudioFocusGainedEvent;
import com.devbrackets.android.exomedia.event.EMAudioFocusLostEvent;
import com.devbrackets.android.exomedia.event.EMMediaCompletionEvent;
import com.devbrackets.android.exomedia.event.EMMediaNextEvent;
import com.devbrackets.android.exomedia.event.EMMediaPlayPauseEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreviousEvent;
import com.devbrackets.android.exomedia.event.EMMediaSeekEndedEvent;
import com.devbrackets.android.exomedia.event.EMMediaSeekStartedEvent;
import com.devbrackets.android.exomedia.event.EMMediaStateEvent;
import com.devbrackets.android.exomedia.event.EMMediaStopEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.util.EMAudioFocusHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

/**
 * TODO: make sure we have full control without bus events
 */
@SuppressWarnings("unused")
public abstract class EMPlaylistService<I extends EMPlaylistManager.PlaylistItem, M extends EMPlaylistManager<I>> extends Service implements
        EMAudioFocusCallback {
    private static final String TAG = "EMPlaylistService";

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

    @Nullable
    protected Bus getBus() {
        return null;
    }

    protected boolean isNetworkAvailable() {
        return true;
    }

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

        audioFocusHelper = null;
        notificationHelper = null;
        lockScreenHelper = null;

        onCreateCalled = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        //This is a workaround for an issue on the Samsung Galaxy S3 (4.4.2) where the onStartCommand will occasionally get called before onCreate
        if (!onCreateCalled) {
            Log.d(TAG, "Service Starting SG 3 4.4.2 Workaround");
            workaroundIntent = intent;
            onCreate();
            return START_NOT_STICKY;
        }

        if (M.ACTION_PLAY.equals(intent.getAction())) {
            startItemPlayback();

            seekToPosition = intent.getIntExtra(M.EXTRA_SEEK_POSITION, -1);
            immediatelyPause = intent.getBooleanExtra(M.EXTRA_START_PAUSED, false);
        } else {
            handleNotificationIntent(intent.getAction(), intent.getExtras());
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
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

    protected void onPlayPauseClickEvent(EMMediaPlayPauseEvent event) {
        if (currentItemIsAudio()) {
            if (audioPlayer.isPlaying() || pausedForFocusLoss) {
                pausedForFocusLoss = false;
                performPause();
            } else {
                performPlay();
            }

            updateNotification();
        }
    }

    protected void onStopEvent(EMMediaStopEvent event) {
        performStop(false);
    }

    protected void onPreviousButtonClickEvent(EMMediaPreviousEvent event) {
        getMediaPlaylistManager().previous();
        startItemPlayback();
        seekToPosition = 0;
    }

    protected void onNextButtonClickEvent(EMMediaNextEvent event) {
        getMediaPlaylistManager().next();
        startItemPlayback();
        seekToPosition = 0;
    }

    protected void onMediaCompletionEvent(EMMediaCompletionEvent event) {
        onNextButtonClickEvent(null);
    }

    protected void onSeekStartedEvent(EMMediaSeekStartedEvent event) {
        if (currentItemIsAudio() && audioPlayer.isPlaying()) {
            pausedForSeek = true;
            audioPlayer.pause();
        }
    }

    protected void onSeekEndedEvent(EMMediaSeekEndedEvent event) {
        if (currentItemIsAudio()) {
            audioPlayer.seekTo(seekToPosition);
            seekToPosition = 0;

            if (pausedForSeek) {
                audioPlayer.start();
                pausedForSeek = false;
            }
        }
    }

    protected void onAudioFocusLostEvent(EMAudioFocusLostEvent event) {
        onAudioFocusLost(event.canDuck());
    }

    protected void onAudioFocusGainedEvent(EMAudioFocusGainedEvent event) {
        onAudioFocusGained();
    }

    protected void onAllowedMediaTypeChangeEvent(EMAllowedMediaTypeChangedEvent event) {
        //We seek through the items until an allowed one is reached, or none is reached and the service is stopped.
        if (event.allowedType != M.MediaType.AUDIO_AND_VIDEO && event.allowedType != currentMediaType) {
            onNextButtonClickEvent(null);
        }
    }

    protected EMPlaylistItemChangedEvent produceEMPlaylistItemChangedEvent() {
        return getMediaItemChangedEvent(null);
    }

    protected EMMediaStateEvent produceMediaStateEvent() {
        return new EMMediaStateEvent(currentState);
    }

    /**
     * Registers an internal {@link com.devbrackets.android.exomedia.service.EMPlaylistService.EventSubscriptionProvider}
     * that will listen to Bus Events and Provide Bus Event related to the {@link EMPlaylistService}.
     *
     * @param bus The bus to register
     */
    protected void registerBus(Bus bus) {
        if (subscriptionProvider == null) {
            subscriptionProvider = new EventSubscriptionProvider<>(this);
        }

        if (bus != null) {
            bus.register(subscriptionProvider);
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

    /**
     * Handles intents from the big notification to control playback
     *
     * @param action The intents action
     * @param extras The intent extras
     */
    private void handleNotificationIntent(String action, Bundle extras) {
        if (action == null || action.isEmpty()) {
            return;
        }

        switch (action) {
            case EMRemoteActions.ACTION_PLAY_PAUSE:
                onPlayPauseClickEvent(null);
                break;

            case EMRemoteActions.ACTION_NEXT:
                onNextButtonClickEvent(null);
                break;

            case EMRemoteActions.ACTION_PREVIOUS:
                onPreviousButtonClickEvent(null);
                break;

            case EMRemoteActions.ACTION_STOP:
                onStopEvent(null);
                break;

            case EMRemoteActions.ACTION_SEEK:
                onSeekEndedEvent(new EMMediaSeekEndedEvent(extras.getInt(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, 0)));
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
        audioPlayer.startProgressPoll(getBus());
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

        Bus bus = getBus();
        if (bus != null) {
            bus.post(getMediaItemChangedEvent(currentItem));
        }

        if (currentItemIsAudio()) {
            audioListener.resetRetryCount();
            playAudioItem();
        } else if (currentItemIsVideo()) {
            playVideoItem();
        } else if (getMediaPlaylistManager().isNextAvailable()) {
            //We get here if there was an error retrieving the currentPlaylistItem
            onNextButtonClickEvent(null);
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

    private void performSeek(int position) {
        if (currentItemIsAudio() && (currentState == MediaState.PLAYING || currentState == MediaState.PAUSED)) {
            audioPlayer.seekTo(position);
        }
    }

    private void performPause() {
        if (currentItemIsAudio()) {
            audioPlayer.pause();
            setMediaState(MediaState.PAUSED);
        }
    }

    private void performPlay() {
        if (currentItemIsAudio()) {
            audioPlayer.start();
            setMediaState(MediaState.PLAYING);
        }
    }

    private void performStop(boolean force) {
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
        getMediaPlaylistManager().setPlayListInfo(-1, false);

        stopSelf();
    }

    protected boolean currentItemIsAudio() {
        return currentPlaylistItem != null && currentPlaylistItem.isAudio();
    }

    protected boolean currentItemIsVideo() {
        return currentPlaylistItem != null && currentPlaylistItem.isVideo();
    }

    private void updateCurrentPlaybackItem() {
        currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
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

    private void setMediaState(MediaState state) {
        currentState = state;

        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaStateEvent(currentState));
        }
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

        updateCurrentPlaybackItem();
    }

    private void setupAsForeground() {
        //Sets up the Lock Screen playback controls
        lockScreenHelper.setLockScreenEnabled(true);
        lockScreenHelper.setLockScreenBaseInformation(getLockScreenIconRes());
        updateLockScreen();

        //Sets up the Notifications
        notificationHelper.setNotificationsEnabled(true);
        notificationHelper.setNotificationBaseInformation(getNotificationId(), getNotificationIconRes(), getClass());
        updateNotification();

        startForeground(getNotificationId(), notificationHelper.getNotification(getNotificationClickPendingIntent()));
        foregroundSetup = true;
    }

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

    private EMPlaylistItemChangedEvent<I> getMediaItemChangedEvent(I currentItem) {
        currentMediaType = getMediaPlaylistManager().getCurrentItemType();
        boolean hasNext = getMediaPlaylistManager().isNextAvailable();
        boolean hasPrevious = getMediaPlaylistManager().isPreviousAvailable();

        //Validates that the currentPlaylistItem is for the currentItem
        if (!getMediaPlaylistManager().isPlayingItem(currentPlaylistItem)) {
            Log.d(TAG, "forcing currentPlaylistItem update");
            updateCurrentPlaybackItem();
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

        return new EMPlaylistItemChangedEvent<>(currentPlaylistItem, currentMediaType, hasNext, hasPrevious);
    }

    protected void onLargeNotificationImageUpdated() {
        updateNotification();
    }

    protected void onLockScreenArtworkUpdated() {
        updateLockScreen();
    }

    /**
     * A class to listen to the EMAudioPlayer events
     */
    private class AudioListener implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        private static final int MAX_RETRY_COUNT = 1;
        private int retryCount = 0;

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (getBus() == null) {
                onMediaCompletionEvent(null);
            }
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
            playlistService.onPlayPauseClickEvent(event);
        }

        @Subscribe
        public void onStopEvent(EMMediaStopEvent event) {
            playlistService.onStopEvent(event);
        }

        @Subscribe
        public void onPreviousButtonClickEvent(EMMediaPreviousEvent event) {
            playlistService.onPreviousButtonClickEvent(event);
        }

        @Subscribe
        public void onNextButtonClickEvent(EMMediaNextEvent event) {
            playlistService.onNextButtonClickEvent(event);
        }

        @Subscribe
        public void onMediaCompletionEvent(EMMediaCompletionEvent event) {
            playlistService.onMediaCompletionEvent(event);
        }

        @Subscribe
        public void onSeekStartedEvent(EMMediaSeekStartedEvent event) {
            playlistService.onSeekStartedEvent(event);
        }

        @Subscribe
        public void onSeekEndedEvent(EMMediaSeekEndedEvent event) {
            playlistService.onSeekEndedEvent(event);
        }

        @Subscribe
        public void onAudioFocusLostEvent(EMAudioFocusLostEvent event) {
            playlistService.onAudioFocusLostEvent(event);
        }

        @Subscribe
        public void onAudioFocusGainedEvent(EMAudioFocusGainedEvent event) {
            playlistService.onAudioFocusGainedEvent(event);
        }

        @Subscribe
        public void onAllowedMediaTypeChangeEvent(EMAllowedMediaTypeChangedEvent event) {
            playlistService.onAllowedMediaTypeChangeEvent(event);
        }

        @Produce
        public EMPlaylistItemChangedEvent produceEMPlaylistItemChangedEvent() {
            return playlistService.produceEMPlaylistItemChangedEvent();
        }

        @Produce
        public EMMediaStateEvent produceMediaStateEvent() {
            return playlistService.produceMediaStateEvent();
        }
    }
}
