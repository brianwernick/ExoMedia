package com.devbrackets.android.exomediademo.ui.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;

import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.helper.VideoItems;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;

import java.util.LinkedList;
import java.util.List;


public class VideoPlayerActivity extends Activity implements MediaPlayer.OnPreparedListener {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)

    protected EMVideoView emVideoView;
    protected PlaylistManager playlistManager;

    protected int selectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        retrieveExtras();
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playlistManager.invokeStop();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Starts the video playback as soon as it is ready
        emVideoView.start();
    }

    /**
     * Retrieves the extra associated with the selected playlist index
     * so that we can start playing the correct item.
     */
    protected void retrieveExtras() {
        Bundle extras = getIntent().getExtras();
        selectedIndex = extras.getInt(EXTRA_INDEX, 0);
    }

    protected void init() {
        setupPlaylistManager();

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        emVideoView.setOnPreparedListener(this);

        playlistManager.setVideoView(emVideoView);
        playlistManager.play(0, false);
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private void setupPlaylistManager() {
        playlistManager = App.getPlaylistManager();

        List<MediaItem> mediaItems = new LinkedList<>();
        for (VideoItems.VideoItem item : VideoItems.getItems()) {
            MediaItem mediaItem = new MediaItem(item);
            mediaItems.add(mediaItem);
        }

        playlistManager.setAllowedMediaType(EMPlaylistManager.MediaType.AUDIO_AND_VIDEO);
        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setPlaylistId(PLAYLIST_ID);
    }
}
