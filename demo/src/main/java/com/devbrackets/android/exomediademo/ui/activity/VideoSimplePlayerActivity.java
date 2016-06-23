package com.devbrackets.android.exomediademo.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.data.Samples;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.exomediademo.playlist.VideoApi;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.google.android.exoplayer.AspectRatioFrameLayout;

import java.util.LinkedList;
import java.util.List;


public class VideoSimplePlayerActivity extends Activity {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)

    protected AspectRatioFrameLayout aspectRatioFrameLayout;
    protected EMVideoView emVideoView;
    protected PlaylistManager playlistManager;

    protected int selectedIndex;
    protected boolean pausedInOnStop = false;

    protected VideoApi videoApi;

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
        if (emVideoView.isPlaying()) {
            pausedInOnStop = true;
            emVideoView.pause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (pausedInOnStop) {
            emVideoView.start();
            pausedInOnStop = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playlistManager.invokeStop();
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

//        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        LayoutInflater inflater = LayoutInflater.from(this);
        View inflatedLayout = null;
        if(playlistManager.getCurrentItem().getSample().getHasDrm()){
            inflatedLayout= inflater.inflate(R.layout.emvideoview_withdrm, null, false); //surfacebacking case
        } else {
            inflatedLayout= inflater.inflate(R.layout.emvideoview_nodrm, null, false);   //textureview
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        addContentView(inflatedLayout, layoutParams);
        emVideoView = (EMVideoView) inflatedLayout.findViewById(R.id.video_play_activity_video_view);

        aspectRatioFrameLayout = (AspectRatioFrameLayout) inflatedLayout.findViewById(R.id.video_frame);
        emVideoView.setAspectRatioView(aspectRatioFrameLayout);


        videoApi = new VideoApi(emVideoView);
        configEMVideoView();

        playlistManager.setVideoPlayer(videoApi);
        playlistManager.play(0, false);
    }

    private void configEMVideoView() {
        emVideoView.getVideoControls().pauseWhenIsSeeking = false; //true to pause while seekTo
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private void setupPlaylistManager() {
        playlistManager = App.getPlaylistManager();

        List<MediaItem> mediaItems = new LinkedList<>();

        mediaItems.add(new MediaItem(Samples.getVideoSamples().get(selectedIndex), false));

        playlistManager.setAllowedMediaType(BasePlaylistManager.AUDIO | BasePlaylistManager.VIDEO);
        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setId(PLAYLIST_ID);
    }

}
