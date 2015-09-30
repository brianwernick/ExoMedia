package com.devbrackets.android.exomediademo.ui.activity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomediademo.R;


public class VideoPlayerActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {
    protected EMVideoView emVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        emVideoView.setOnPreparedListener(this);

        //For now we just picked an arbitrary item to play.  More can be found at
        //https://archive.org/details/more_animation
        emVideoView.setVideoURI(Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Starts the video playback as soon as it is ready
        emVideoView.start();
    }
}
