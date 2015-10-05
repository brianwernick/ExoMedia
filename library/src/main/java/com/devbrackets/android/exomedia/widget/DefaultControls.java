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

package com.devbrackets.android.exomedia.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.event.EMMediaNextEvent;
import com.devbrackets.android.exomedia.event.EMMediaPlayPauseEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreviousEvent;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMVideoViewControlVisibilityEvent;
import com.devbrackets.android.exomedia.listener.EMVideoViewControlsCallback;
import com.devbrackets.android.exomedia.util.EMEventBus;
import com.devbrackets.android.exomedia.util.EMResourceUtil;

/**
 * This is a simple abstraction for the EMVideoView to have a single "View" to add
 * or remove for the Default Video Controls.
 */
public abstract class DefaultControls extends RelativeLayout {
    public static final int DEFAULT_CONTROL_HIDE_DELAY = 2000;
    protected static final long CONTROL_VISIBILITY_ANIMATION_LENGTH = 300;
    protected static final int INVALID_RESOURCE_ID = 0;

    public interface SeekCallbacks {
        boolean onSeekStarted();
        boolean onSeekEnded(int seekTime);
    }

    protected TextView currentTime;
    protected TextView endTime;
    protected ImageButton playPauseButton;
    protected ImageButton previousButton;
    protected ImageButton nextButton;
    protected ViewGroup controlsContainer;
    protected ProgressBar loadingProgress;

    protected Drawable defaultPlayDrawable;
    protected Drawable defaultPauseDrawable;
    protected Drawable defaultPreviousDrawable;
    protected Drawable defaultNextDrawable;

    //Since the Play/Pause button uses 2 separate resource Id's we need to store them
    protected int playResourceId = INVALID_RESOURCE_ID;
    protected int pauseResourceId = INVALID_RESOURCE_ID;

    protected long hideDelay = -1;

    protected boolean isVisible = true;
    protected boolean canViewHide = true;
    protected Handler visibilityHandler = new Handler();

    protected boolean busPostHandlesEvent = false;

    @Nullable
    protected EMEventBus bus;

    protected EMVideoView videoView;
    protected SeekCallbacks seekCallbacks;
    protected EMVideoViewControlsCallback callback;

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
     * @param bus The EventBus to dispatch events on
     */
    public void setBus(@Nullable EMEventBus bus) {
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
        loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        controlsContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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
    public abstract void setPosition(long position);

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    public abstract void setDuration(long duration);

    /**
     * Performs the progress update on the current time field,
     * and the seek bar
     *
     * @param event The most recent progress
     */
    public abstract void setProgressEvent(EMMediaProgressEvent event);

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
     * Sets the state list drawable resource id to use for the Rewind button.
     * <b><em>NOTE:</em></b> The Rewind button is only shown on TV layouts
     *
     * @param resourceId The resourceId or 0
     */
    public void setRewindImageResource(@DrawableRes int resourceId) {
        //Purposefully left blank
    }

    /**
     * Sets the state list drawable resource id to use for the Fast Forward button.
     * <b><em>NOTE:</em></b> The Fast Forward button is only shown on TV layouts
     *
     * @param resourceId The resourceId or 0
     */
    public void setFastForwardImageResource(@DrawableRes int resourceId) {
        //Purposefully left blank
    }

    /**
     * Makes sure the playPause button represents the correct playback state
     *
     * @param isPlaying If the video is currently playing
     */
    public void updatePlayPauseImage(boolean isPlaying) {
        if (isPlaying) {
            if (pauseResourceId != INVALID_RESOURCE_ID) {
                playPauseButton.setImageResource(pauseResourceId);
            } else {
                playPauseButton.setImageDrawable(defaultPauseDrawable);
            }
        } else {
            if (playResourceId != INVALID_RESOURCE_ID) {
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
     * <p>
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
     * <p>
     * This method will NOT re-add buttons that have previously been removed with
     * {@link #setPreviousButtonRemoved(boolean)}.
     *
     * @param enabled If the Next button is enabled [default: false]
     */
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    /**
     * Sets the button state for the Rewind button.  This will just
     * change the images specified with {@link #setRewindImageResource(int)},
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
     * change the images specified with {@link #setFastForwardImageResource(int)},
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
     * Retrieves the view references from the xml layout
     */
    protected void retrieveViews() {
        currentTime = (TextView) findViewById(R.id.exomedia_controls_current_time);
        endTime = (TextView) findViewById(R.id.exomedia_controls_end_time);
        playPauseButton = (ImageButton) findViewById(R.id.exomedia_controls_play_pause_btn);
        previousButton = (ImageButton) findViewById(R.id.exomedia_controls_previous_btn);
        nextButton = (ImageButton) findViewById(R.id.exomedia_controls_next_btn);
        loadingProgress = (ProgressBar) findViewById(R.id.exomedia_controls_video_loading);
        controlsContainer = (ViewGroup) findViewById(R.id.exomedia_controls_interactive_container);
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
        defaultPlayDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_play_arrow_white, R.color.exomedia_default_controls_button_selector);

        defaultPauseDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_pause_white, R.color.exomedia_default_controls_button_selector);
        playPauseButton.setImageDrawable(defaultPlayDrawable);

        defaultPreviousDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_skip_previous_white, R.color.exomedia_default_controls_button_selector);
        previousButton.setImageDrawable(defaultPreviousDrawable);

        defaultNextDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_skip_next_white, R.color.exomedia_default_controls_button_selector);
        nextButton.setImageDrawable(defaultNextDrawable);
    }

    /**
     * Performs the functionality when the PlayPause button is clicked.  This
     * includes invoking the callback method if it is enabled, posting the bus
     * event, and toggling the video playback.
     */
    protected void onPlayPauseClick() {
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

    /**
     * Performs the functionality to inform any listeners that the previous
     * button has been clicked
     */
    protected void onPreviousClick() {
        if (callback != null && callback.onPreviousClicked()) {
            return;
        }

        if (bus != null) {
            bus.post(new EMMediaPreviousEvent());
        }
    }

    /**
     * Performs the functionality to inform any listeners that the next
     * button has been clicked
     */
    protected void onNextClick() {
        if (callback != null && callback.onNextClicked()) {
            return;
        }

        if (bus != null) {
            bus.post(new EMMediaNextEvent());
        }
    }

    /**
     * Used to retrieve the layout resource identifier to inflate
     *
     * @return The layout resource identifier to inflate
     */
    @LayoutRes
    protected abstract int getLayoutResource();

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
     * Performs the functionality to inform the callback and post bus events
     * that the DefaultControls visibility has changed
     */
    protected void onVisibilityChanged() {
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
    protected void animateVisibility(boolean toVisible) {
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
}