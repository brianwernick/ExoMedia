package com.devbrackets.android.exomediademo;

import android.app.Application;
import android.os.StrictMode;

import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.squareup.leakcanary.LeakCanary;

public class App extends Application {

    private static App application;
    private static PlaylistManager playlistManager;

    @Override
    public void onCreate() {
        enableStrictMode();
        super.onCreate();

        application = this;
        playlistManager = new PlaylistManager();
        LeakCanary.install(this);
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

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }
}
