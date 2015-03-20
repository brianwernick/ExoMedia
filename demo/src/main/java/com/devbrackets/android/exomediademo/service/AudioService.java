package com.devbrackets.android.exomediademo.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.EMNotification;
import com.devbrackets.android.exomedia.listener.EMAudioFocusCallback;
import com.devbrackets.android.exomedia.util.EMAudioFocusHelper;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.StartupActivity;
import com.devbrackets.android.exomediademo.helper.PlayListManager;

public class AudioService extends Service implements EMAudioFocusCallback, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {
    public static final String ACTION_PLAY = "AudioService.Play";

    private static final int FOREGROUND_REQUEST_CODE = 0;
    private static final int NOTIFICATION_ID = 1564; //Arbitrary
    private static final float AUDIO_DUCK_VOLUME = 0.1f;

    private boolean pausedForFocusLoss = false;
    private PlayListManager playListManager = new PlayListManager();

    private EMAudioPlayer audioPlayer;
    private EMNotification notificationHelper;
    private EMAudioFocusHelper audioFocusHelper;
    private EMNotification.NotificationMediaState notificationMediaState = new EMNotification.NotificationMediaState();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationHelper = new EMNotification(getApplicationContext());
        audioFocusHelper = new EMAudioFocusHelper(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        relaxResources(true);
        releaseAudioFocus();
        notificationHelper = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        if (ACTION_PLAY.equals(intent.getAction())) {
            startItemPlayback();
        } else {
            handleNotificationIntent(intent.getAction());
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

    /**
     * When we get the audio focus make sure to resume the audio, or return the volume to normal
     */
    @Override
    public boolean onAudioFocusGained() {
        if (!audioPlayer.isPlaying() && pausedForFocusLoss) {
            audioPlayer.start();
        } else {
            audioPlayer.setVolume(1.0f, 1.0f); //reset the audio volume
        }

        return true;
    }

    /**
     * When we loose audio focus make sure to either turn the volume down, or pause the playback
     */
    @Override
    public boolean onAudioFocusLost(boolean canDuckAudio) {
        if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_NO_DUCK) {
            if (audioPlayer.isPlaying()) {
                pausedForFocusLoss = true;
                audioPlayer.pause();
            }
        } else {
            audioPlayer.setVolume(AUDIO_DUCK_VOLUME, AUDIO_DUCK_VOLUME);
        }

        return true;
    }

    /**
     * Once an audio item completes, move to the next audio item.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        performNext();
    }

    /**
     * If there was an error playing an audio item then stop the service
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        performStop();
        return false;
    }

    /**
     * Once the audioPlayer is ready start the audio and update the notification
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        startAudioPlayer();
        updateNotification();
    }

    /**
     * Handles intents from the big notification to control playback
     *
     * @param action The intents action
     */
    private void handleNotificationIntent(String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        switch (action) {
            case EMNotification.ACTION_PLAY_PAUSE:
                performPlayPause();
                break;
            case EMNotification.ACTION_NEXT:
                performNext();
                break;
            case EMNotification.ACTION_PREVIOUS:
                performPrevious();
                break;
            case EMNotification.ACTION_CLOSE:
                performStop();
                break;
            default:
                break;
        }
    }

    /**
     * From the notification, we want to update the visual state of the notification and change
     * the current playback.
     */
    private void performPlayPause() {
        if (audioPlayer.isPlaying() || pausedForFocusLoss) {
            pausedForFocusLoss = false;
            audioPlayer.pause();
        } else {
            audioPlayer.start();
        }

        updateNotification();
    }

    /**
     * From the notification, we want to go to the next audio item
     */
    private void performNext() {
        playListManager.getNextAudioUrl();
        startItemPlayback();
    }

    /**
     * From the notification, we want to go to the previous audio item
     */
    private void performPrevious() {
        playListManager.getPreviousAudioUrl();
        startItemPlayback();
    }

    /**
     * Performs the EMAudioPlayer setup
     */
    private void initializeAudioPlayer() {
        if (audioPlayer != null) {
            audioPlayer.reset();
            return;
        }

        audioPlayer = new EMAudioPlayer(getApplicationContext());
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnErrorListener(this);
        audioPlayer.setOnCompletionListener(this);
    }

    /**
     * If possible we will play the current item, otherwise stop the service
     */
    private void startItemPlayback() {
        if (playListManager.getCurrentAudioUrl() != null) {
            playAudioItem();
        } else {
            //At this point there is nothing for us to play, so we stop the service
            performStop();
        }
    }

    /**
     * Plays the current item in teh playListManager.
     */
    private void playAudioItem() {
        initializeAudioPlayer();
        obtainAudioFocus();

        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioPlayer.setDataSource(this, Uri.parse(playListManager.getCurrentAudioUrl()));

        setupAsForeground();
        audioPlayer.prepareAsync();
    }

    /**
     * Stops the AudioService, cleaning up any objects that need to be removed
     */
    private void performStop() {
        // let go of all resources
        relaxResources(true);
        releaseAudioFocus();

        stopSelf();
    }

    /**
     * Attempts to obtain the audio focus so that we can start/resume playback
     */
    private void obtainAudioFocus() {
        audioFocusHelper.requestFocus();
    }

    /**
     * If we currently have the audio focus, then release it.
     */
    private void releaseAudioFocus() {
        audioFocusHelper.abandonFocus();
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
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
            }

            return;
        } else if (audioFocusHelper.getCurrentAudioFocus() == EMAudioFocusHelper.Focus.NO_FOCUS_CAN_DUCK) {
            audioPlayer.setVolume(AUDIO_DUCK_VOLUME, AUDIO_DUCK_VOLUME);
        } else {
            audioPlayer.setVolume(1.0f, 1.0f); //Full volume
        }

        if (!audioPlayer.isPlaying()) {
            audioPlayer.start();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks, and the audioPlayer if requested
     *
     * @param releaseAudioPlayer True if the audioPlayer should be released
     */
    private void relaxResources(boolean releaseAudioPlayer) {
        stopForeground(true);

        if (releaseAudioPlayer && audioPlayer != null) {
            audioPlayer.reset();
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    /**
     * Performs the initial procedures to activate the foreground notification
     */
    private void setupAsForeground() {
        notificationHelper.setNotificationsEnabled(true);
        notificationHelper.setNotificationBaseInformation(NOTIFICATION_ID, R.drawable.ic_notification_icon, AudioService.class);
        updateNotification();

        //The PendingIntent is what will be opened when the notification is clicked (any area other than the playback controls)
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), FOREGROUND_REQUEST_CODE,
                new Intent(getApplicationContext(), StartupActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        startForeground(NOTIFICATION_ID, notificationHelper.getNotification(pi));
    }

    /**
     * Updates the notification information, setting the image, title, the playback control states
     */
    private void updateNotification() {
        if (audioPlayer == null) {
            return;
        }

        String title = "EMAudioPlayer Demo";

        notificationMediaState.setNextEnabled(playListManager.isNextAvailable());
        notificationMediaState.setPreviousEnabled(playListManager.isPreviousAvailable());
        notificationMediaState.setPlaying(audioPlayer.isPlaying());

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        notificationHelper.updateNotificationInformation(getString(R.string.app_name), title, bitmap, notificationMediaState);
    }
}
