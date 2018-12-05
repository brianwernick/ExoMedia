package com.devbrackets.android.exomediademo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ListView

import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.adapter.StartupListAdapter

class StartupActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.startup_activity)

        val exampleList = findViewById<ListView>(R.id.startup_activity_list)
        exampleList.adapter = StartupListAdapter(this)
        exampleList.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        when (position) {
            StartupListAdapter.INDEX_AUDIO_PLAYBACK -> showAudioSelectionActivity()
            StartupListAdapter.INDEX_VIDEO_PLAYBACK -> showVideoSelectionActivity()
        }
    }

    private fun showVideoSelectionActivity() {
        startActivity(Intent(this, VideoSelectionActivity::class.java))
    }

    private fun showAudioSelectionActivity() {
        startActivity(Intent(this, AudioSelectionActivity::class.java))
    }
}
