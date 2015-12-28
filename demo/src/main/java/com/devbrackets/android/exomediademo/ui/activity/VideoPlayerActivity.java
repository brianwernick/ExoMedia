package com.devbrackets.android.exomediademo.ui.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.helper.VideoItems;


public class VideoPlayerActivity extends Activity implements MediaPlayer.OnPreparedListener {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    protected EMVideoView emVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        emVideoView.setOnPreparedListener(this);

        //For now we just picked an arbitrary item to play.  More can be found at
        //https://archive.org/details/more_animation
        emVideoView.setVideoURI(Uri.parse(VideoItems.getItems().get(getIntent().getIntExtra(EXTRA_INDEX, 0)).getMediaUrl()));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Starts the video playback as soon as it is ready
        emVideoView.start();
    }
}
