package com.devbrackets.android.exomediademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomediademo.helper.AudioItems;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.exomediademo.service.AudioService;

import java.util.LinkedList;
import java.util.List;

//TODO: once the loading is completed remove the progress bar...
public class AudioPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final int PLAYLIST_ID = 4; //Arbitrary, for the example

    private ProgressBar loadingBar;
    private ImageView artworkView;

    private TextView currentPositionView;
    private TextView durationView;

    private SeekBar seekBar;

    private ImageButton previousButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;

    private PlaylistManager playlistManager;
    private int selectedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_player_activity);

        retrieveExtras();
        init();
    }

    private void retrieveExtras() {
        Bundle extras = getIntent().getExtras();
        selectedIndex = extras.getInt(EXTRA_INDEX, 0);
    }

    private void init() {
        retrieveViews();
        setupListeners();

        boolean generatedPlaylist = setupPlaylistManager();
        startPlayback(generatedPlaylist);
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

        List<PlaylistManager.MediaItem> mediaItems = new LinkedList<>();
        for (AudioItems.AudioItem item : AudioItems.getItems()) {
            PlaylistManager.MediaItem mediaItem = new PlaylistManager.MediaItem(item);
            mediaItems.add(mediaItem);
        }

        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setPlaylistId(PLAYLIST_ID);

        return true;
    }

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
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekPosition = seekBar.getProgress();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            playlistManager.invokeSeek(seekPosition);
            seekPosition = -1;
        }
    }
}
