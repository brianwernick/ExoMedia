package com.devbrackets.android.exomediademo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.adapter.VideoSelectionListAdapter;

/**
 * A simple activity that allows the user to select a
 * chapter form "The Count of Monte Cristo" to play
 * (limited to chapters 1 - 4).
 */
public class VideoSelectionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static final String FULLSCREEN_EXTRA = "fullscreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_selection_activity);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.title_video_selection_activity));
        }

        ListView exampleList = (ListView) findViewById(R.id.selection_activity_list);
        exampleList.setAdapter(new VideoSelectionListAdapter(this));
        exampleList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startAudioPlayerActivity(position);
    }

    private void startAudioPlayerActivity(int selectedIndex) {
        Intent intent;
        if(getIntent().getBooleanExtra(FULLSCREEN_EXTRA, false)) {
            intent = new Intent(this, FullScreenVideoPlayerActivity.class);
        } else {
            intent = new Intent(this, VideoPlayerActivity.class);
        }
        intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, selectedIndex);
        startActivity(intent);
    }
}