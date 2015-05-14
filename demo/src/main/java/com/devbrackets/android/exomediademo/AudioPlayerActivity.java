package com.devbrackets.android.exomediademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMPlaylistServiceCallback;
import com.devbrackets.android.exomedia.listener.EMProgressCallback;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.service.EMPlaylistService;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.helper.AudioItems;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.exomediademo.service.AudioService;
import com.squareup.picasso.Picasso;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * An example activity to show how to implement and audio UI
 * that interacts with the {@link EMPlaylistService} and {@link EMPlaylistManager}
 * classes.
 */
public class AudioPlayerActivity extends AppCompatActivity implements EMPlaylistServiceCallback, EMProgressCallback {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final int PLAYLIST_ID = 4; //Arbitrary, for the example

    private ProgressBar loadingBar;
    private ImageView artworkView;

    private TextView currentPositionView;
    private TextView durationView;

    private SeekBar seekBar;
    private boolean shouldSetDuration;
    private boolean userInteracting;

    private ImageButton previousButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;

    private PlaylistManager playlistManager;
    private int selectedIndex = 0;

    private StringBuilder formatBuilder;
    private Formatter formatter;

    private Picasso picasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_player_activity);

        retrieveExtras();
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playlistManager.unRegisterServiceCallbacks(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playlistManager = App.getPlaylistManager();
        playlistManager.registerServiceCallbacks(this);

        //Makes sure to retrieve the current playback information
        updateCurrentPlaybackInformation();
    }

    @Override
    public boolean onPlaylistItemChanged(EMPlaylistManager.PlaylistItem currentItem, EMPlaylistManager.MediaType mediaType, boolean hasNext, boolean hasPrevious) {
        shouldSetDuration = true;

        //Updates the button states
        nextButton.setEnabled(hasNext);
        previousButton.setEnabled(hasPrevious);

        //Loads the new image
        picasso.load(currentItem.getArtworkUrl()).into(artworkView);

        return true;
    }

    @Override
    public boolean onMediaStateChanged(EMPlaylistService.MediaState mediaState) {
        switch (mediaState) {
            case RETRIEVING:
            case PREPARING:
                restartLoading();
                break;

            case PLAYING:
                doneLoading(true);
                break;

            case PAUSED:
                doneLoading(false);
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onProgressUpdated(EMMediaProgressEvent event) {
        if (shouldSetDuration && event.getDuration() > 0) {
            shouldSetDuration = false;
            setDuration(event.getDuration());
        }

        if (!userInteracting) {
            seekBar.setSecondaryProgress((int) (event.getDuration() * event.getBufferPercentFloat()));
            seekBar.setProgress((int)event.getPosition());
            currentPositionView.setText(formatTime(event.getPosition()));
        }

        return true;
    }

    /**
     * Makes sure to update the UI to the current playback item.
     */
    private void updateCurrentPlaybackInformation() {
        EMPlaylistItemChangedEvent itemChangedEvent = playlistManager.getCurrentItemChangedEvent();
        if (itemChangedEvent != null) {
            onPlaylistItemChanged(itemChangedEvent.currentItem, itemChangedEvent.mediaType, itemChangedEvent.hasNext, itemChangedEvent.hasPrevious);
        }

        onMediaStateChanged(playlistManager.getCurrentMediaState());

        EMMediaProgressEvent progressEvent = playlistManager.getCurrentProgress();
        if (progressEvent != null) {
            onProgressUpdated(progressEvent);
        }
    }

    /**
     * Retrieves the extra associated with the selected playlist index
     * so that we can start playing the correct item.
     */
    private void retrieveExtras() {
        Bundle extras = getIntent().getExtras();
        selectedIndex = extras.getInt(EXTRA_INDEX, 0);
    }

    /**
     * Performs the initialization of the views and any other
     * general setup
     */
    private void init() {
        retrieveViews();
        setupListeners();

        picasso = Picasso.with(getApplicationContext());

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        boolean generatedPlaylist = setupPlaylistManager();
        startPlayback(generatedPlaylist);
    }


    /**
     * Called when we receive a notification that the current item is
     * done loading.  This will then update the view visibilities and
     * states accordingly.
     *
     * @param isPlaying True if the audio item is currently playing
     */
    private void doneLoading(boolean isPlaying) {
        loadCompleted();
        updatePlayPauseImage(isPlaying);
    }

    /**
     * Updates the Play/Pause image to represent the correct playback state
     *
     * @param isPlaying True if the audio item is currently playing
     */
    private void updatePlayPauseImage(boolean isPlaying) {
        int resId = isPlaying ? R.drawable.exomedia_ic_pause_black : R.drawable.exomedia_ic_play_arrow_black;
        playPauseButton.setImageResource(resId);
    }

    /**
     * Used to inform the controls to finalize their setup.  This
     * means replacing the loading animation with the PlayPause button
     */
    public void loadCompleted() {
        playPauseButton.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE );

        loadingBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Used to inform the controls to return to the loading stage.
     * This is the opposite of {@link #loadCompleted()}
     */
    public void restartLoading() {
        playPauseButton.setVisibility(View.INVISIBLE);
        previousButton.setVisibility(View.INVISIBLE);
        nextButton.setVisibility(View.INVISIBLE );

        loadingBar.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the {@link #seekBar}s max and updates the duration text
     *
     * @param duration The duration of the media item in milliseconds
     */
    private void setDuration(long duration) {
        seekBar.setMax((int)duration);
        durationView.setText(formatTime(duration));
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     *
     * @return True if the content was generated
     */
    private boolean setupPlaylistManager() {
        playlistManager = App.getPlaylistManager();

        //There is nothing to do if the currently playing values are the same
        if (playlistManager.getPlayListId() == PLAYLIST_ID) {
            return false;
        }

        //Create and setup the playlist
        playlistManager.setMediaServiceClass(AudioService.class);

        List<MediaItem> mediaItems = new LinkedList<>();
        for (AudioItems.AudioItem item : AudioItems.getItems()) {
            MediaItem mediaItem = new MediaItem(item);
            mediaItems.add(mediaItem);
        }

        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setPlaylistId(PLAYLIST_ID);

        return true;
    }

    /**
     * Populates the class variables with the views created from the
     * xml layout file.
     */
    private void retrieveViews() {
        loadingBar = (ProgressBar)findViewById(R.id.audio_player_loading);
        artworkView = (ImageView)findViewById(R.id.audio_player_image);

        currentPositionView = (TextView)findViewById(R.id.audio_player_position);
        durationView = (TextView)findViewById(R.id.audio_player_duration);

        seekBar = (SeekBar)findViewById(R.id.audio_player_seek);

        previousButton = (ImageButton)findViewById(R.id.audio_player_previous);
        playPauseButton = (ImageButton)findViewById(R.id.audio_player_play_pause);
        nextButton = (ImageButton)findViewById(R.id.audio_player_next);
    }

    /**
     * Links the SeekBarChanged to the {@link #seekBar} and
     * onClickListeners to the media buttons that call the appropriate
     * invoke methods in the {@link #playlistManager}
     */
    private void setupListeners() {
        seekBar.setOnSeekBarChangeListener(new SeekBarChanged());

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.invokePrevious();
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.invokePausePlay();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistManager.invokeNext();
            }
        });
    }

    /**
     * Starts the audio playback if necessary.
     *
     * @param forceStart True if the audio should be started from the beginning even if it is currently playing
     */
    private void startPlayback(boolean forceStart) {
        //If we are changing audio files, or we haven't played before then start the playback
        if (forceStart || playlistManager.getCurrentIndex() != selectedIndex) {
            playlistManager.play(0, false);
        }
    }

    /**
     * Formats the specified millisecond time to a human readable format
     * in the form of (Hours : Minutes : Seconds)
     *
     * @param time The time in milliseconds to format
     * @return The human readable time
     */
    private String formatTime(long time) {
        long seconds = (time % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS;
        long minutes = (time % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS;
        long hours = (time % DateUtils.DAY_IN_MILLIS) / DateUtils.HOUR_IN_MILLIS;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        }

        return formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    /**
     * Listens to the seek bar change events and correctly handles the changes
     */
    private class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {
        private int seekPosition = -1;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            seekPosition = progress;
            currentPositionView.setText(formatTime(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            userInteracting = true;

            seekPosition = seekBar.getProgress();
            playlistManager.invokeSeekStarted();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            userInteracting = false;

            playlistManager.invokeSeekEnded(seekPosition);
            seekPosition = -1;
        }
    }
}
