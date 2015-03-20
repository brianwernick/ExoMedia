package com.devbrackets.android.exomediademo.helper;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple object to keep track of audio items to play
 * and the current playback item
 */
public class PlayListManager {
    private List<String> audioUrls;
    private int index = 0;

    public PlayListManager() {
        audioUrls = new ArrayList<>();
        audioUrls.add("http://html5demos.com/assets/dizzy.mp4");
        audioUrls.add("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear0/prog_index.m3u8");
    }

    public int getIndex() {
        return index;
    }

    @Nullable
    public String getCurrentAudioUrl() {
        if (index >= 0 && index < audioUrls.size()) {
            return audioUrls.get(index);
        }

        return null;
    }

    @Nullable
    public String getNextAudioUrl() {
        index++;
        return getCurrentAudioUrl();
    }

    @Nullable
    public String getPreviousAudioUrl() {
        index--;
        return getCurrentAudioUrl();
    }

    public boolean isNextAvailable() {
        return index < audioUrls.size() -1;
    }

    public boolean isPreviousAvailable() {
        return index > 0;
    }
}
