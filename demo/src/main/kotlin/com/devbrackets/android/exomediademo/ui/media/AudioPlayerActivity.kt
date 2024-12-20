package com.devbrackets.android.exomediademo.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.ui.media.audio.AudioPlayerScreen
import com.devbrackets.android.exomediademo.ui.media.audio.AudioPlayerViewModel

/**
 * An example activity to show how to implement and audio UI
 * that interacts with the [com.devbrackets.android.playlistcore.service.BasePlaylistService]
 * and [com.devbrackets.android.playlistcore.manager.ListPlaylistManager]
 * classes.
 */
class AudioPlayerActivity : AppCompatActivity() {
  companion object {
    const val EXTRA_INDEX = "EXTRA_INDEX"
    const val PLAYLIST_ID = 4 //Arbitrary, for the example

    fun intent(context: Context, sample: Samples.Sample): Intent {
      // NOTE:
      // We pass the index of the sample for simplicity, however you will likely
      // want to pass an ID for both the selected playlist (audio/video in this demo)
      // and the selected media item
      val index = Samples.audio.indexOf(sample)

      return Intent(context, AudioPlayerActivity::class.java).apply {
        putExtra(EXTRA_INDEX, index)
      }
    }
  }

  private val viewModel: AudioPlayerViewModel by viewModels {
    AudioPlayerViewModel.factory(applicationContext)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      AudioPlayerScreen(
        viewModel = viewModel,
        onBackClicked = ::onBackPressed
      )
    }

    viewModel.startPlayback()
  }

  override fun onResume() {
    super.onResume()
    viewModel.connectPlaylist()
  }
}
