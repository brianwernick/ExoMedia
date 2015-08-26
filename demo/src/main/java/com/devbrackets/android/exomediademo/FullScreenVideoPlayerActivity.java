package com.devbrackets.android.exomediademo;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.devbrackets.android.exomedia.listener.EMVideoViewControlsCallback;

/**
 * A simple example of making a fullscreen video player activity.
 * <p>
 * <b><em>NOTE:</em></b> the EMVideoView setup is done in the {@link VideoPlayerActivity}
 */
public class FullScreenVideoPlayerActivity extends VideoPlayerActivity {
    private FullScreenListener fullScreenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            fullScreenListener = new FullScreenListener();
        }

        goFullscreen();
        emVideoView.setVideoViewControlsCallback(new DefaultControlsCallback());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exitFullscreen();
    }

    private void goFullscreen() {
        setUiFlags(true);
    }

    private void exitFullscreen() {
        setUiFlags(false);
    }

    /**
     * Applies the correct flags to the windows decor view to enter
     * or exit fullscreen mode
     *
     * @param fullscreen True if entering fullscreen mode
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setUiFlags(boolean fullscreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View decorView = getWindow().getDecorView();
            if (decorView != null) {
                decorView.setSystemUiVisibility(fullscreen ? getFullscreenUiFlags() : View.SYSTEM_UI_FLAG_VISIBLE);
                decorView.setOnSystemUiVisibilityChangeListener(fullScreenListener);
            }
        }
    }

    /**
     * Determines the appropriate fullscreen flags based on the
     * systems API version.
     *
     * @return The appropriate decor view flags to enter fullscreen mode when supported
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private int getFullscreenUiFlags() {
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        return flags;
    }

    /**
     * Listens to the system to determine when to show the default controls
     * for the {@link com.devbrackets.android.exomedia.EMVideoView}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class FullScreenListener implements View.OnSystemUiVisibilityChangeListener {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                emVideoView.showDefaultControls();
            }
        }
    }

    /**
     * A Listener for the {@link com.devbrackets.android.exomedia.DefaultControls}
     * so that we can re-enter fullscreen mode when the controls are hidden.
     */
    private class DefaultControlsCallback implements EMVideoViewControlsCallback {
        @Override
        public boolean onPlayPauseClicked() {
            return false; // No additional functionality performed
        }

        @Override
        public boolean onPreviousClicked() {
            return false; // No additional functionality performed
        }

        @Override
        public boolean onNextClicked() {
            return false; // No additional functionality performed
        }

        @Override
        public boolean onControlsShown() {
            return false; // No additional functionality performed
        }

        @Override
        public boolean onControlsHidden() {
            goFullscreen();
            return false;
        }
    }
}
