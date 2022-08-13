package com.devbrackets.android.exomediademo

import android.app.Application
import com.devbrackets.android.exomediademo.playlist.manager.PlaylistManager

class App : Application() {
  val playlistManager: PlaylistManager by lazy {
    PlaylistManager(this)
  }
}
