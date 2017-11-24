/*
 * Copyright (C) 2016 Brian Wernick
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

package com.devbrackets.android.exomedia.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.listener.VideoControlsSeekListener;
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener;
import com.devbrackets.android.exomedia.util.Repeater;
import com.devbrackets.android.exomedia.util.ResourceUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * This is a simple abstraction for the {@link VideoView} to have a single "View" to add
 * or remove for the Default Video Controls.
 */
@SuppressWarnings("unused")
public abstract class VideoControls extends RelativeLayout {
    public static final int DEFAULT_CONTROL_HIDE_DELAY = 2_000;
    protected static final long CONTROL_VISIBILITY_ANIMATION_LENGTH = 300;

    protected TextView currentTimeTextView;
    protected TextView endTimeTextView;

    protected TextView titleTextView;
    protected TextView subTitleTextView;
    protected TextView descriptionTextView;

    protected ImageButton playPauseButton;
    protected ImageButton previousButton;
    protected ImageButton nextButton;

    protected ProgressBar loadingProgressBar;

    protected ViewGroup controlsContainer;
    protected ViewGroup textContainer;

    protected Drawable playDrawable;
    protected Drawable pauseDrawable;

    @NonNull
    protected Handler visibilityHandler = new Handler();
    @NonNull
    protected Repeater progressPollRepeater = new Repeater();

    @Nullable
    protected VideoView videoView;

    @Nullable
    protected VideoControlsSeekListener seekListener;
    @Nullable
    protected VideoControlsButtonListener buttonsListener;
    @Nullable
    protected VideoControlsVisibilityListener visibilityListener;

    @NonNull
    protected InternalListener internalListener = new InternalListener();

    @NonNull
    protected SparseBooleanArray enabledViews = new SparseBooleanArray();

    protected long hideDelay = DEFAULT_CONTROL_HIDE_DELAY;

    protected boolean isLoading = false;
    protected boolean isVisible = true;
    protected boolean canViewHide = true;
    protected boolean hideEmptyTextContainer = true;

    /**
     * Sets the current video position, updating the seek bar
     * and the current time field
     *
     * @param position The position in milliseconds
     */
    public abstract void setPosition(@IntRange(from = 0) long position);

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    public abstract void setDuration(@IntRange(from = 0) long duration);

    /**
     * Performs the progress update on the current time field,
     * and the seek bar
     */
    public abstract void updateProgress(@IntRange(from = 0) long position, @IntRange(from = 0) long duration, @IntRange(from = 0, to = 100) int bufferPercent);

    /**
     * Used to retrieve the layout resource identifier to inflate
     *
     * @return The layout resource identifier to inflate
     */
    @LayoutRes
    protected abstract int getLayoutResource();

    /**
     * Performs the control visibility animation for showing or hiding
     * this view
     * @param toVisible True if the view should be visible at the end of the animation
     */
    protected abstract void animateVisibility(boolean toVisible);

    /**
     * Update the current visibility of the text block independent of
     * the controls visibility
     */
    protected abstract void updateTextContainerVisibility();

    /**
     * Update the controls to indicate that the video
     * is loading.
     *
     * @param initialLoad <code>True</code> if the loading is the initial state, not for seeking or buffering
     */
    public abstract void showLoading(boolean initialLoad);

    /**
     * Update the controls to indicate that the video is no longer loading
     * which will re-display the play/pause, progress, etc. controls
     */
    public abstract void finishLoading();

    public VideoControls(Context context) {
        super(context);
        setup(context);
    }

    public VideoControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public VideoControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //A poll used to periodically update the progress bar
        progressPollRepeater.setRepeatListener(new Repeater.RepeatListener() {
            @Override
            public void onRepeat() {
                updateProgress();
            }
        });

        if (videoView != null && videoView.isPlaying()) {
            updatePlaybackState(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        progressPollRepeater.stop();
        progressPollRepeater.setRepeatListener(null);
    }

    /**
     * Sets the parent view to use for determining playback length, position,
     * state, etc.  This should only be called once, during the setup process
     *
     * @param VideoView The Parent view to these controls
     */
    public void setVideoView(@Nullable VideoView VideoView) {
        this.videoView = VideoView;
    }

    /**
     * Sets the callbacks to inform of progress seek events
     *
     * @param callbacks The callbacks to inform
     */
    public void setSeekListener(@Nullable VideoControlsSeekListener callbacks) {
        this.seekListener = callbacks;
    }

    /**
     * Specifies the callback to inform of button click events
     *
     * @param callback The callback
     */
    public void setButtonListener(@Nullable VideoControlsButtonListener callback) {
        this.buttonsListener = callback;
    }

    /**
     * Sets the callbacks to inform of visibility changes
     *
     * @param callbacks The callbacks to inform
     */
    public void setVisibilityListener(@Nullable VideoControlsVisibilityListener callbacks) {
        this.visibilityListener = callbacks;
    }

    /**
     * Informs the controls that the playback state has changed.  This will
     * update to display the correct views, and manage progress polling.
     *
     * @param isPlaying True if the media is currently playing
     */
    public void updatePlaybackState(boolean isPlaying) {
        updatePlayPauseImage(isPlaying);
        progressPollRepeater.start();

        if (isPlaying) {
            hideDelayed();
        } else {
            show();
        }
    }

    /**
     * Sets the title to display for the current item in playback
     *
     * @param title The title to display
     */
    public void setTitle(@Nullable CharSequence title) {
        titleTextView.setText(title);
        updateTextContainerVisibility();
    }

    /**
     * Sets the subtitle to display for the current item in playback.  This will be displayed
     * as the second line of text
     *
     * @param subTitle The sub title to display
     */
    public void setSubTitle(@Nullable CharSequence subTitle) {
        subTitleTextView.setText(subTitle);
        updateTextContainerVisibility();
    }

    /**
     * Sets the description text to display for the current item in playback.  This will be displayed
     * as the third line of text and unlike the {@link #setTitle(CharSequence)} and {@link #setSubTitle(CharSequence)}
     * this text wont be limited to a single line of text
     *
     * @param description The artist to display
     */
    public void setDescription(@Nullable CharSequence description) {
        descriptionTextView.setText(description);
        updateTextContainerVisibility();
    }

    /**
     * Sets the drawables to use for the PlayPause button
     *
     * @param playDrawable The drawable to represent play
     * @param pauseDrawable The drawable to represent pause
     */
    public void setPlayPauseDrawables(Drawable playDrawable, Drawable pauseDrawable) {
        this.playDrawable = playDrawable;
        this.pauseDrawable = pauseDrawable;

        updatePlayPauseImage(videoView != null && videoView.isPlaying());
    }

    /**
     * Sets the drawable for the previous button
     *
     * @param drawable The drawable to use
     */
    public void setPreviousDrawable(Drawable drawable) {
        previousButton.setImageDrawable(drawable);
    }

    /**
     * Sets the drawable for the next button
     *
     * @param drawable The drawable to use
     */
    public void setNextDrawable(Drawable drawable) {
        nextButton.setImageDrawable(drawable);
    }

    /**
     * Sets the drawable for the rewind button
     *
     * @param drawable The drawable to use
     */
    public void setRewindDrawable(Drawable drawable) {
        //Purposefully let blank
    }

    /**
     * Sets the drawable for the Fast  button
     *
     * @param drawable The drawable to use
     */
    public void setFastForwardDrawable(Drawable drawable) {
        //Purposefully let blank
    }

    /**
     * Makes sure the playPause button represents the correct playback state
     *
     * @param isPlaying If the video is currently playing
     */
    public void updatePlayPauseImage(boolean isPlaying) {
        playPauseButton.setImageDrawable(isPlaying ? pauseDrawable : playDrawable);
    }

    /**
     * Sets the button state for the Previous button.  This will just
     * change the images specified with {@link #setPreviousDrawable(Drawable)},
     * or use the defaults if they haven't been set, and block any click events.
     * <p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setNextButtonRemoved(boolean)}.
     *
     * @param enabled If the Previous button is enabled [default: false]
     */
    public void setPreviousButtonEnabled(boolean enabled) {
        previousButton.setEnabled(enabled);
        enabledViews.put(R.id.exomedia_controls_previous_btn, enabled);
    }

    /**
     * Sets the button state for the Next button.  This will just
     * change the images specified with {@link #setNextDrawable(Drawable)},
     * or use the defaults if they haven't been set, and block any click events.
     * <p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setPreviousButtonRemoved(boolean)}.
     *
     * @param enabled If the Next button is enabled [default: false]
     */
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        enabledViews.put(R.id.exomedia_controls_next_btn, enabled);
    }

    /**
     * Sets the button state for the Rewind button.  This will just
     * change the images specified with {@link #setRewindDrawable(Drawable)},
     * or use the defaults if they haven't been set
     * <p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setRewindButtonRemoved(boolean)}.
     *
     * @param enabled If the Rewind button is enabled [default: false]
     */
    public void setRewindButtonEnabled(boolean enabled) {
        //Purposefully left blank
    }

    /**
     * Sets the button state for the Fast Forward button.  This will just
     * change the images specified with {@link #setFastForwardDrawable(Drawable)},
     * or use the defaults if they haven't been set
     * <p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setFastForwardButtonRemoved(boolean)}.
     *
     * @param enabled If the Rewind button is enabled [default: false]
     */
    public void setFastForwardButtonEnabled(boolean enabled) {
        //Purposefully left blank
    }

    /**
     * Adds or removes the Previous button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setPreviousButtonEnabled(boolean)}
     *
     * @param removed If the Previous button should be removed [default: true]
     */
    public void setPreviousButtonRemoved(boolean removed) {
        previousButton.setVisibility(removed ? View.GONE : View.VISIBLE);
    }

    /**
     * Adds or removes the Next button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setNextButtonEnabled(boolean)}
     *
     * @param removed If the Next button should be removed [default: true]
     */
    public void setNextButtonRemoved(boolean removed) {
        nextButton.setVisibility(removed ? View.GONE : View.VISIBLE);
    }

    /**
     * Adds or removes the Rewind button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setRewindButtonEnabled(boolean)}
     *
     * @param removed If the Rewind button should be removed [default: true]
     */
    public void setRewindButtonRemoved(boolean removed) {
        //Purposefully left blank
    }

    /**
     * Adds or removes the FastForward button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setFastForwardButtonEnabled(boolean)}
     *
     * @param removed If the FastForward button should be removed [default: true]
     */
    public void setFastForwardButtonRemoved(boolean removed) {
        //Purposefully left blank
    }

    public void addExtraView(@NonNull View view) {
        //Purposefully left blank
    }

    public void removeExtraView(@NonNull View view) {
        //Purposefully left blank
    }

    @NonNull
    public List<View> getExtraViews() {
        return new LinkedList<>();
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
     * Immediately starts the animation to hide the controls
     */
    public void hide() {
        if (!canViewHide || isLoading) {
            return;
        }

        //Makes sure we don't have a separate hide animation scheduled
        visibilityHandler.removeCallbacksAndMessages(null);
        clearAnimation();

        animateVisibility(false);
    }

    /**
     * After the specified delay the view will be hidden.  If the user is interacting
     * with the controls then we wait until after they are done to start the delay.
     */
    public void hideDelayed() {
        hideDelayed(hideDelay);
    }

    /**
     * After the specified delay the view will be hidden.  If the user is interacting
     * with the controls then we wait until after they are done to start the delay.
     *
     * @param delay The delay in milliseconds to wait to start the hide animation
     */
    public void hideDelayed(long delay) {
        hideDelay = delay;

        if (delay < 0 || !canViewHide || isLoading) {
            return;
        }

        visibilityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, delay);
    }

    /**
     * Sets the delay to use when hiding the controls via the {@link #hideDelayed()}
     * method. This value will be overridden if {@link #hideDelayed(long)} is called.
     *
     * @param delay The delay in milliseconds to wait to start the hide animation
     */
    public void setHideDelay(long delay) {
        hideDelay = delay;
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
     * Sets weather the text block and associated container will be hidden
     * when no content is specified.
     *
     * @param hide If the empty text blocks can be hidden [default: true]
     */
    public void setHideEmptyTextContainer(boolean hide) {
        this.hideEmptyTextContainer = hide;
        updateTextContainerVisibility();
    }

    /**
     * Returns <code>true</code> if the {@link VideoControls} are visible
     *
     * @return <code>true</code> if the controls are visible
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Retrieves the view references from the xml layout
     */
    protected void retrieveViews() {
        currentTimeTextView = findViewById(R.id.exomedia_controls_current_time);
        endTimeTextView = findViewById(R.id.exomedia_controls_end_time);

        titleTextView = findViewById(R.id.exomedia_controls_title);
        subTitleTextView = findViewById(R.id.exomedia_controls_sub_title);
        descriptionTextView = findViewById(R.id.exomedia_controls_description);

        playPauseButton = findViewById(R.id.exomedia_controls_play_pause_btn);
        previousButton = findViewById(R.id.exomedia_controls_previous_btn);
        nextButton = findViewById(R.id.exomedia_controls_next_btn);

        loadingProgressBar = findViewById(R.id.exomedia_controls_video_loading);

        controlsContainer = findViewById(R.id.exomedia_controls_interactive_container);
        textContainer = findViewById(R.id.exomedia_controls_text_container);
    }

    /**
     * Registers any internal listeners to perform the playback controls,
     * such as play/pause, next, and previous
     */
    protected void registerListeners() {
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
    }

    /**
     * Updates the drawables used for the buttons to AppCompatTintDrawables
     */
    protected void updateButtonDrawables() {
        playDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_play_arrow_white, R.color.exomedia_default_controls_button_selector);
        pauseDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_pause_white, R.color.exomedia_default_controls_button_selector);
        playPauseButton.setImageDrawable(playDrawable);

        Drawable previousDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_skip_previous_white, R.color.exomedia_default_controls_button_selector);
        previousButton.setImageDrawable(previousDrawable);

        Drawable nextDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_skip_next_white, R.color.exomedia_default_controls_button_selector);
        nextButton.setImageDrawable(nextDrawable);
    }

    /**
     * Performs the functionality when the PlayPause button is clicked.  This
     * includes invoking the callback method if it is enabled, posting the bus
     * event, and toggling the video playback.
     */
    protected void onPlayPauseClick() {
        if (buttonsListener == null || !buttonsListener.onPlayPauseClicked()) {
            internalListener.onPlayPauseClicked();
        }
    }

    /**
     * Performs the functionality to inform any listeners that the previous
     * button has been clicked
     */
    protected void onPreviousClick() {
        if (buttonsListener == null || !buttonsListener.onPreviousClicked()) {
            internalListener.onPreviousClicked();
        }
    }

    /**
     * Performs the functionality to inform any listeners that the next
     * button has been clicked
     */
    protected void onNextClick() {
        if (buttonsListener == null || !buttonsListener.onNextClicked()) {
            internalListener.onNextClicked();
        }
    }

    /**
     * Performs any initialization steps such as retrieving views, registering listeners,
     * and updating any drawables.
     *
     * @param context The context to use for retrieving the correct layout
     */
    protected void setup(Context context) {
        View.inflate(context, getLayoutResource(), this);
        retrieveViews();

        registerListeners();
        updateButtonDrawables();
    }

    /**
     * Determines if the <code>textContainer</code> doesn't have any text associated with it
     * @return True if there is no text contained in the views in the <code>textContainer</code>
     */
    @SuppressWarnings("RedundantIfStatement")
    protected boolean isTextContainerEmpty() {
        if (titleTextView.getText() != null && titleTextView.getText().length() > 0) {
            return false;
        }

        if (subTitleTextView.getText() != null && subTitleTextView.getText().length() > 0) {
            return false;
        }

        if (descriptionTextView.getText() != null && descriptionTextView.getText().length() > 0) {
            return false;
        }

        return true;
    }

    /**
     * Performs the functionality to inform the callback
     * that the DefaultControls visibility has changed
     */
    protected void onVisibilityChanged() {
        if (visibilityListener == null) {
            return;
        }

        if (isVisible) {
            visibilityListener.onControlsShown();
        } else {
            visibilityListener.onControlsHidden();
        }
    }

    /**
     * Called by the {@link #progressPollRepeater} to update the progress
     * bar using the {@link #videoView} to retrieve the correct information
     */
    protected void updateProgress() {
        if (videoView != null) {
            updateProgress(videoView.getCurrentPosition(), videoView.getDuration(), videoView.getBufferPercentage());
        }
    }

    /**
     * An internal class used to handle the default functionality for the
     * VideoControls
     */
    protected class InternalListener implements VideoControlsSeekListener, VideoControlsButtonListener {
        protected boolean pausedForSeek = false;

        @Override
        public boolean onPlayPauseClicked() {
            if (videoView == null) {
                return false;
            }

            if (videoView.isPlaying()) {
                videoView.pause();
            } else {
                videoView.start();
            }

            return true;
        }

        @Override
        public boolean onPreviousClicked() {
            //Purposefully left blank
            return false;
        }

        @Override
        public boolean onNextClicked() {
            //Purposefully left blank
            return false;
        }

        @Override
        public boolean onRewindClicked() {
            //Purposefully left blank
            return false;
        }

        @Override
        public boolean onFastForwardClicked() {
            //Purposefully left blank
            return false;
        }

        @Override
        public boolean onSeekStarted() {
            if (videoView == null) {
                return false;
            }

            if (videoView.isPlaying()) {
                pausedForSeek = true;
                videoView.pause(true);
            }

            show();
            return true;
        }

        @Override
        public boolean onSeekEnded(long seekTime) {
            if (videoView == null) {
                return false;
            }

            videoView.seekTo(seekTime);

            if (pausedForSeek) {
                pausedForSeek = false;
                videoView.start();
                hideDelayed();
            }

            return true;
        }
    }
}