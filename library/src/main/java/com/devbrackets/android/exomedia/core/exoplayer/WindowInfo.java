package com.devbrackets.android.exomedia.core.exoplayer;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.Timeline;

public class WindowInfo {
    public final int previousWindowIndex;
    public final int currentWindowIndex;
    public final int nextWindowIndex;

    @NonNull
    public final Timeline.Window currentWindow;

    public WindowInfo(int previousWindowIndex, int currentWindowIndex, int nextWindowIndex, @NonNull Timeline.Window currentWindow) {
        this.previousWindowIndex = previousWindowIndex;
        this.currentWindowIndex = currentWindowIndex;
        this.nextWindowIndex = nextWindowIndex;
        this.currentWindow = currentWindow;
    }
}