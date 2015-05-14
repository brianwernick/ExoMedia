package com.devbrackets.android.exomediademo;

import android.app.Application;

import com.devbrackets.android.exomediademo.manager.PlaylistManager;

public class App extends Application {

    private static App application;
    private static PlaylistManager playlistManager;

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;
        playlistManager = new PlaylistManager();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        application = null;
        playlistManager = null;
    }

    public static PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public static App getApplication() {
        return application;
    }
}
