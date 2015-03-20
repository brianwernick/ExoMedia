package com.devbrackets.android.exomediademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;

import com.devbrackets.android.exomediademo.service.AudioService;


public class AudioPlayerActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_player_activity);

        Button button = (Button)findViewById(R.id.audio_player_activity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Starts the audio service
                Intent intent = new Intent(AudioPlayerActivity.this, AudioService.class);
                intent.setAction(AudioService.ACTION_PLAY);
                AudioPlayerActivity.this.startService(intent);
            }
        });
    }
}
