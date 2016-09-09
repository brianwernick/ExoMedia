package com.devbrackets.android.exomedia.ui.widget;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.view.View;

public interface VideoControls<T extends View> {
    T getView();

    /**
     * Sets the parent view to use for determining playback length, position,
     * state, etc.  This should only be called once, during the setup process
     *
     * @param EMVideoView The Parent view to these controls
     */
    void setVideoView(@Nullable EMVideoView EMVideoView);

    /**
     * Immediately starts the animation to show the controls
     */
    void show();

    /**
     * View will be hidden after a delay (depending on implementation). If the user is interacting
     * with the controls, implentation should wait until after they are done to start the delay.
     */
    void hideDelayed();

    /**
     * Update the controls to indicate that the video
     * is loading.
     *
     * @param initialLoad <code>True</code> if the loading is the initial state, not for seeking or buffering
     */
    void showLoading(boolean initialLoad);

    /**
     * Informs the controls that the playback state has changed.  This will
     * update to display the correct views, and manage progress polling.
     *
     * @param isPlaying True if the media is currently playing
     */
    void updatePlaybackState(boolean isPlaying);

    /**
     * Update the controls to indicate that the video is no longer loading
     * which will re-display the play/pause, progress, etc. controls
     */
    void finishLoading();

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    void setDuration(@IntRange(from = 0) long duration);
}
