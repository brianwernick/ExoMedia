package com.devbrackets.android.exomediademo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import com.devbrackets.android.exomedia.EMVideoView;


public class VideoPlayerActivity extends ActionBarActivity implements MediaPlayer.OnPreparedListener {
    protected EMVideoView emVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        emVideoView.setDefaultControlsEnabled(true);
        emVideoView.setOnPreparedListener(this);

        //For now we just picked an arbitrary item to play.  More can be found at
        //https://github.com/google/ExoPlayer/blob/master/demo/src/main/java/com/google/android/exoplayer/demo/Samples.java
        emVideoView.setVideoURI(Uri.parse("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Starts the video playback as soon as it is ready
        emVideoView.start();
    }
}
