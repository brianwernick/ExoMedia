package com.devbrackets.android.exomediademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.devbrackets.android.exomediademo.adapter.StartupListAdapter;

public class StartupActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private ListView exampleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup_activity);

        exampleList = (ListView)findViewById(R.id.startup_activity_list);
        exampleList.setAdapter(new StartupListAdapter(this));
        exampleList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case StartupListAdapter.INDEX_VIDEO_PLAYBACK:
                startVideoPlayerActivity();

            default:
        }
    }

    private void startVideoPlayerActivity() {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        startActivity(intent);
    }
}
