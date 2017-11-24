package com.devbrackets.android.exomediademo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.adapter.StartupListAdapter;

public class StartupActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup_activity);

        ListView exampleList = findViewById(R.id.startup_activity_list);
        exampleList.setAdapter(new StartupListAdapter(this));
        exampleList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case StartupListAdapter.INDEX_AUDIO_PLAYBACK:
                showAudioSelectionActivity();
                break;

            case StartupListAdapter.INDEX_VIDEO_PLAYBACK:
                showVideoSelectionActivity();
                break;

            default:
        }
    }

    private void showVideoSelectionActivity() {
        Intent intent = new Intent(this, VideoSelectionActivity.class);
        startActivity(intent);
    }

    private void showAudioSelectionActivity() {
        Intent intent = new Intent(this, AudioSelectionActivity.class);
        startActivity(intent);
    }
}
