package com.devbrackets.android.exomediademo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ListView

import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.adapter.SampleListAdapter
import com.devbrackets.android.exomediademo.data.Samples

/**
 * A simple activity that allows the user to select a
 * video to play
 */
class VideoSelectionActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_selection_activity)

        supportActionBar?.title = resources.getString(R.string.title_video_selection_activity)

        val exampleList = findViewById<ListView>(R.id.selection_activity_list)
        exampleList.adapter = SampleListAdapter(this, Samples.getVideoSamples())
        exampleList.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        startVideoPlayerActivity(position)
    }

    private fun startVideoPlayerActivity(selectedIndex: Int) {
        startActivity(Intent(this, FullScreenVideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_INDEX, selectedIndex)
        })
    }
}