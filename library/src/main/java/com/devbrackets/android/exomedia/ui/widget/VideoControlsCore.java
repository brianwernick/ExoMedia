package com.devbrackets.android.exomedia.ui.widget;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

/**
 * An Interface that represents the core VideoControl functionality that
 * the {@link VideoView} uses to inform the controls of updated states, etc.
 *
 * Note: To preserve backwards compatibility this is being called *Core,
 * On the next major release we should rename this to VideoControls and
 * call the abstract class something else.
 */
public interface VideoControlsCore {

    /**
     * Called when the controls have been registered by the
     * {@link VideoView}.
     *
     * @param videoView The {@link VideoView} that the controls are attached to
     */
    void onAttachedToView(@NonNull VideoView videoView);

    /**
     * Called when the controls have been cleaned up on the {@link VideoView}
     * side in preparation for detachment.
     *
     * @param videoView The {@link VideoView} that the controls are detaching from
     */
    void onDetachedFromView(@NonNull VideoView videoView);

    /**
     * Shows the controls immediately
     */
    void show();

    /**
     * Hides the controls immediately if <code>delayed</code> is false
     * otherwise a delay determined by the implementation will be used
     * before the controls are hidden.  If the user is interacting with
     * the controls then we wait until after they are done to start the
     * delay.
     */
    void hide(boolean delayed);

    /**
     * Update the controls to indicate that the video
     * is loading.
     *
     * @param initialLoad <code>true</code> if the loading is the initial state, not for seeking or buffering
     */
    void showLoading(boolean initialLoad);

    /**
     * Update the controls to indicate that the video is no longer loading
     * which will re-display the play/pause, progress, etc. controls
     */
    void finishLoading();

    /**
     * Informs the controls that the playback state has changed.  This will
     * update to display the correct views, and manage progress polling.
     *
     * @param isPlaying True if the media is currently playing
     */
    void updatePlaybackState(boolean isPlaying);

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    void setDuration(@IntRange(from = 0) long duration);

    /**
     * Returns <code>true</code> if the {@link VideoControlsCore} are visible
     *
     * @return <code>true</code> if the controls are visible
     */
    boolean isVisible();
}
