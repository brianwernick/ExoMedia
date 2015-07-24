/*
 * Copyright (C) 2015 Brian Wernick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomedia.event.EMMediaNextEvent;
import com.devbrackets.android.exomedia.event.EMMediaPlayPauseEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreviousEvent;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMVideoViewControlVisibilityEvent;
import com.devbrackets.android.exomedia.listener.EMVideoViewControlsCallback;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;
import com.squareup.otto.Bus;

/**
 * This is a simple abstraction for the EMVideoView to have a single "View" to add
 * or remove for the Default Video Controls.
 */
public class DefaultControls extends RelativeLayout {
    private static final long CONTROL_VISIBILITY_ANIMATION_LENGTH = 300;

    public interface SeekCallbacks {
        boolean onSeekStarted();
        boolean onSeekEnded(int seekTime);
    }

    private TextView currentTime;
    private TextView endTime;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ProgressBar loadingProgress;

    private EMVideoViewControlsCallback callback;
    private boolean busPostHandlesEvent = false;

    private Drawable defaultPlayDrawable;
    private Drawable defaultPauseDrawable;
    private Drawable defaultPreviousDrawable;
    private Drawable defaultNextDrawable;

    //Remember, 0 is not a valid resourceId
    private int playResourceId = 0;
    private int pauseResourceId = 0;

    private boolean previousButtonRemoved = true;
    private boolean nextButtonRemoved = true;

    private boolean pausedForSeek = false;
    private long hideDelay = -1;
    private boolean userInteracting = false;

    private boolean isVisible = true;
    private boolean canViewHide = true;
    private Handler visibilityHandler = new Handler();

    private EMVideoView videoView;
    private Bus bus;

    private SeekCallbacks seekCallbacks;

    public DefaultControls(Context context) {
        super(context);
        setup(context);
    }

    public DefaultControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DefaultControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DefaultControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context);
    }

    /**
     * Sets the bus to use for dispatching Events that correspond to the callbacks
     * listed in {@link com.devbrackets.android.exomedia.listener.EMVideoViewControlsCallback}
     *
     * @param bus The Otto bus to dispatch events on
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    /**
     * Sets the parent view to use for determining playback length, position,
     * state, etc.  This should only be called once, during the setup process
     *
     * @param EMVideoView The Parent view to these controls
     */
    public void setVideoView(EMVideoView EMVideoView) {
        this.videoView = EMVideoView;
    }

    /**
     * Specifies the callback to use for informing the host app of click events
     *
     * @param callback The callback
     */
    public void setVideoViewControlsCallback(EMVideoViewControlsCallback callback) {
        this.callback = callback;
    }

    /**
     * Used to update the control view visibilities to indicate that the video
     * is loading.  This is different from using {@link #loadCompleted()} and {@link #restartLoading()}
     * because those update additional information.
     *
     * @param isLoading True if loading progress should be shown
     */
    public void setLoading(boolean isLoading) {
        playPauseButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        previousButton.setVisibility(isLoading || previousButtonRemoved ? View.INVISIBLE : View.VISIBLE);
        nextButton.setVisibility(isLoading || nextButtonRemoved ? View.INVISIBLE : View.VISIBLE);
        loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Used to inform the controls to finalize their setup.  This
     * means replacing the loading animation with the PlayPause button
     */
    public void loadCompleted() {
        setLoading(false);
        updatePlayPauseImage(videoView.isPlaying());
    }

    /**
     * Used to inform the controls to return to the loading stage.
     * This is the opposite of {@link #loadCompleted()}
     */
    public void restartLoading() {
        setLoading(true);
    }

    /**
     * Sets the callbacks to inform of progress seek events
     *
     * @param callbacks The callbacks to inform
     */
    public void setSeekCallbacks(@Nullable SeekCallbacks callbacks) {
        this.seekCallbacks = callbacks;
    }

    /**
     * Sets the current video position, updating the seek bar
     * and the current time field
     *
     * @param position The position in milliseconds
     */
    public void setPosition(long position) {
        currentTime.setText(TimeFormatUtil.formatMs(position));
        seekBar.setProgress((int) position);
    }

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    public void setDuration(long duration) {
        if (duration != seekBar.getMax()) {
            endTime.setText(TimeFormatUtil.formatMs(duration));
            seekBar.setMax((int) duration);
        }
    }

    /**
     * Performs the progress update on the current time field,
     * and the seek bar
     *
     * @param event The most recent progress
     */
    public void setProgressEvent(EMMediaProgressEvent event) {
        if (!userInteracting) {
            seekBar.setSecondaryProgress((int) (seekBar.getMax() * event.getBufferPercentFloat()));
            seekBar.setProgress((int) event.getPosition());
            currentTime.setText(TimeFormatUtil.formatMs(event.getPosition()));
        }
    }

    /**
     * Sets the resource id's to use for the PlayPause button.
     *
     * @param playResourceId  The resourceId or 0
     * @param pauseResourceId The resourceId or 0
     */
    public void setPlayPauseImages(@DrawableRes int playResourceId, @DrawableRes int pauseResourceId) {
        this.playResourceId = playResourceId;
        this.pauseResourceId = pauseResourceId;

        updatePlayPauseImage(videoView != null && videoView.isPlaying());
    }

    /**
     * Sets the state list drawable resource id to use for the Previous button.
     *
     * @param resourceId The resourceId or 0
     */
    public void setPreviousImageResource(@DrawableRes int resourceId) {
        if (resourceId != 0) {
            previousButton.setImageResource(resourceId);
        } else {
            previousButton.setImageDrawable(defaultPreviousDrawable);
        }
    }

    /**
     * Sets the state list drawable resource id to use for the Next button.
     *
     * @param resourceId The resourceId or 0
     */
    public void setNextImageResource(@DrawableRes int resourceId) {
        if (resourceId != 0) {
            nextButton.setImageResource(resourceId);
        } else {
            nextButton.setImageDrawable(defaultNextDrawable);
        }
    }

    /**
     * Makes sure the playPause button represents the correct playback state
     *
     * @param isPlaying If the video is currently playing
     */
    public void updatePlayPauseImage(boolean isPlaying) {
        if (isPlaying) {
            if (pauseResourceId != 0) {
                playPauseButton.setImageResource(pauseResourceId);
            } else {
                playPauseButton.setImageDrawable(defaultPauseDrawable);
            }
        } else {
            if (playResourceId != 0) {
                playPauseButton.setImageResource(playResourceId);
            } else {
                playPauseButton.setImageDrawable(defaultPlayDrawable);
            }
        }
    }

    /**
     * Sets the button state for the Previous button.  This will just
     * change the images specified with {@link #setPreviousImageResource(int)},
     * or use the defaults if they haven't been set, and block any click events.
     * </p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setNextButtonRemoved(boolean)}.
     *
     * @param enabled If the Previous button is enabled [default: false]
     */
    public void setPreviousButtonEnabled(boolean enabled) {
        previousButton.setEnabled(enabled);
    }

    /**
     * Sets the button state for the Next button.  This will just
     * change the images specified with {@link #setNextImageResource(int)},
     * or use the defaults if they haven't been set, and block any click events.
     * </p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setPreviousButtonRemoved(boolean)}.
     *
     * @param enabled If the Next button is enabled [default: false]
     */
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    /**
     * Adds or removes the Previous button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setPreviousButtonEnabled(boolean)}
     *
     * @param removed If the Previous button should be removed [default: true]
     */
    public void setPreviousButtonRemoved(boolean removed) {
        previousButton.setVisibility(removed ? View.INVISIBLE : View.VISIBLE);
        previousButtonRemoved = removed;
    }

    /**
     * Adds or removes the Next button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setNextButtonEnabled(boolean)}
     *
     * @param removed If the Next button should be removed [default: true]
     */
    public void setNextButtonRemoved(boolean removed) {
        nextButton.setVisibility(removed ? View.INVISIBLE : View.VISIBLE);
        nextButtonRemoved = removed;
    }

    /**
     * Immediately starts the animation to show the controls
     */
    public void show() {
        //Makes sure we don't have a hide animation scheduled
        visibilityHandler.removeCallbacksAndMessages(null);
        clearAnimation();

        animateVisibility(true);
    }

    /**
     * After the specified delay the view will be hidden.  If the user is interacting
     * with the controls then we wait until after they are done to start the delay.
     *
     * @param delay The delay in milliseconds to wait to start the hide animation
     */
    public void hideDelayed(long delay) {
        hideDelay = delay;

        if (delay < 0 || !canViewHide) {
            return;
        }

        //If the user is interacting with controls we don't want to start the delayed hide yet
        if (userInteracting) {
            return;
        }

        visibilityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateVisibility(false);
            }
        }, delay);
    }

    /**
     * Sets weather this control can be hidden.
     *
     * @param canHide If this control can be hidden [default: true]
     */
    public void setCanHide(boolean canHide) {
        canViewHide = canHide;
    }

    /**
     * Sets weather the control functionality should treat the button clicks
     * as handled when a bus event is posted.  This is to make Bus events
     * act like the callbacks set with {@link #setVideoViewControlsCallback(EMVideoViewControlsCallback)}
     *
     * @param finish True if the Bus events should act as handling the button clicks
     */
    public void setFinishOnBusEvents(boolean finish) {
        busPostHandlesEvent = finish;
    }

    /**
     * Updates the drawables used for the buttons to AppCompatTintDrawables
     */
    private void updateButtonDrawables() {
        defaultPlayDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_play_arrow_white));
        DrawableCompat.setTintList(defaultPlayDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));

        defaultPauseDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_pause_white));
        DrawableCompat.setTintList(defaultPauseDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));
        playPauseButton.setImageDrawable(defaultPlayDrawable);

        defaultPreviousDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_skip_previous_white));
        DrawableCompat.setTintList(defaultPreviousDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));
        previousButton.setImageDrawable(defaultPreviousDrawable);

        defaultNextDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_skip_next_white));
        DrawableCompat.setTintList(defaultNextDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));
        nextButton.setImageDrawable(defaultNextDrawable);
    }

    private Drawable getDrawable(@DrawableRes int resourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getResources().getDrawable(resourceId, getContext().getTheme());
        }

        //noinspection deprecation - depreciated in API 22
        return getResources().getDrawable(resourceId);
    }

    /**
     * Performs the functionality when the PlayPause button is clicked.  This
     * includes invoking the callback method if it is enabled, posting the bus
     * event, and toggling the video playback.
     */
    private void onPlayPauseClick() {
        if (callback != null && callback.onPlayPauseClicked()) {
            return;
        }

        if (bus != null) {
            bus.post(new EMMediaPlayPauseEvent());
            if (busPostHandlesEvent) {
                return;
            }
        }

        //toggles the playback
        boolean playing = videoView.isPlaying();
        if (playing) {
            videoView.pause();
        } else {
            videoView.start();
        }
    }

    private void onPreviousClick() {
        if (callback != null && callback.onPreviousClicked()) {
            return;
        }

        if (bus != null) {
            bus.post(new EMMediaPreviousEvent());
        }
    }

    private void onNextClick() {
        if (callback != null && callback.onNextClicked()) {
            return;
        }

        if (bus != null) {
            bus.post(new EMMediaNextEvent());
        }
    }

    private void setup(Context context) {
        View.inflate(context, R.layout.exomedia_video_controls_overlay, this);

        currentTime = (TextView) findViewById(R.id.exomedia_controls_current_time);
        endTime = (TextView) findViewById(R.id.exomedia_controls_end_time);
        seekBar = (SeekBar) findViewById(R.id.exomedia_controls_video_seek);
        playPauseButton = (ImageButton) findViewById(R.id.exomedia_controls_play_pause_btn);
        previousButton = (ImageButton) findViewById(R.id.exomedia_controls_previous_btn);
        nextButton = (ImageButton) findViewById(R.id.exomedia_controls_next_btn);
        loadingProgress = (ProgressBar) findViewById(R.id.exomedia_controls_video_loading);

        seekBar.setOnSeekBarChangeListener(new SeekBarChanged());

        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPauseClick();
            }
        });
        previousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPreviousClick();
            }
        });
        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClick();
            }
        });

        updateButtonDrawables();
    }

    /**
     * Performs the functionality to inform the callback and post bus events
     * that the DefaultControls visibility has changed
     */
    private void onVisibilityChanged() {
        boolean handled = false;
        if (callback != null) {
            if (isVisible) {
                handled = callback.onControlsShown();
            } else {
                handled = callback.onControlsHidden();
            }
        }

        if (!handled && bus != null) {
            bus.post(new EMVideoViewControlVisibilityEvent(isVisible));
        }
    }

    /**
     * Performs the control visibility animation for showing or hiding
     * this view
     *
     * @param toVisible True if the view should be visible at the end of the animation
     */
    private void animateVisibility(boolean toVisible) {
        if (isVisible == toVisible) {
            return;
        }

        float startAlpha = toVisible ? 0 : 1;
        float endAlpha = toVisible ? 1 : 0;

        AlphaAnimation animation = new AlphaAnimation(startAlpha, endAlpha);
        animation.setDuration(CONTROL_VISIBILITY_ANIMATION_LENGTH);
        animation.setFillAfter(true);
        startAnimation(animation);

        isVisible = toVisible;
        onVisibilityChanged();
    }

    /**
     * Listens to the seek bar change events and correctly handles the changes
     */
    private class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {
        private int seekToTime;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            seekToTime = progress;
            if (seekCallbacks != null && seekCallbacks.onSeekStarted()) {
                return;
            }

            if (currentTime != null) {
                currentTime.setText(TimeFormatUtil.formatMs(progress));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            userInteracting = true;

            if (videoView.isPlaying()) {
                pausedForSeek = true;
                videoView.pause();
            }

            //Make sure to keep the controls visible during seek
            show();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            userInteracting = false;
            if (seekCallbacks != null && seekCallbacks.onSeekEnded(seekToTime)) {
                return;
            }

            videoView.seekTo(seekToTime);

            if (pausedForSeek) {
                pausedForSeek = false;
                videoView.start();
                hideDelayed(hideDelay);
            }
        }
    }
}