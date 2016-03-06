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
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.ui.animation.BottomViewHideShowAnimation;
import com.devbrackets.android.exomedia.util.EMResourceUtil;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;

/**
 * Provides playback controls for the EMVideoView on TV devices.
 */
@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoControlsLeanback extends VideoControls {
    protected static final int FAST_FORWARD_REWIND_AMOUNT = 10000; //10 seconds

    protected ProgressBar progressBar;

    protected ImageView rippleIndicator;

    protected ImageButton fastForwardButton;
    protected ImageButton rewindButton;

    protected Drawable defaultRewindDrawable;
    protected Drawable defaultFastForwardDrawable;

    protected View currentFocus;
    protected ButtonFocusChangeListener buttonFocusChangeListener = new ButtonFocusChangeListener();

    public VideoControlsLeanback(Context context) {
        super(context);
    }

    public VideoControlsLeanback(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoControlsLeanback(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoControlsLeanback(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void setup(Context context) {
        super.setup(context);
        internalListener = new LeanbackInternalListener();
        registerForInput();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        playPauseButton.requestFocus();
        currentFocus = playPauseButton;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.exomedia_default_controls_leanback;
    }

    /**
     * Sets the current video position, updating the seek bar
     * and the current time field
     *
     * @param position The position in milliseconds
     */
    @Override
    public void setPosition(long position) {
        currentTime.setText(TimeFormatUtil.formatMs(position));
        progressBar.setProgress((int) position);
    }

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    @Override
    public void setDuration(long duration) {
        if (duration != progressBar.getMax()) {
            endTime.setText(TimeFormatUtil.formatMs(duration));
            progressBar.setMax((int) duration);
        }
    }

    @Override
    public void updateProgress(@IntRange(from = 0) long position, @IntRange(from = 0) long duration, @IntRange(from = 0, to = 100) int bufferPercent) {
        progressBar.setSecondaryProgress((int) (progressBar.getMax() * ((float)bufferPercent / 100)));
        progressBar.setProgress((int) position);
        currentTime.setText(TimeFormatUtil.formatMs(position));
    }

    /**
     * Sets the state list drawable resource id to use for the Rewind button.
     * <b><em>NOTE:</em></b> The Rewind button is only shown on TV layouts
     *
     * @param resourceId The resourceId or 0
     */
    @Override
    public void setRewindImageResource(@DrawableRes int resourceId) {
        if (rewindButton == null) {
            return;
        }

        if (resourceId != 0) {
            rewindButton.setImageResource(resourceId);
        } else {
            rewindButton.setImageDrawable(defaultRewindDrawable);
        }
    }

    /**
     * Sets the state list drawable resource id to use for the Fast Forward button.
     * <b><em>NOTE:</em></b> The Fast Forward button is only shown on TV layouts
     *
     * @param resourceId The resourceId or 0
     */
    @Override
    public void setFastForwardImageResource(@DrawableRes int resourceId) {
        if (fastForwardButton == null) {
            return;
        }

        if (resourceId != 0) {
            fastForwardButton.setImageResource(resourceId);
        } else {
            fastForwardButton.setImageDrawable(defaultFastForwardDrawable);
        }
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
    @Override
    public void setRewindButtonEnabled(boolean enabled) {
        if (rewindButton != null) {
            rewindButton.setEnabled(enabled);
        }
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
    @Override
    public void setFastForwardButtonEnabled(boolean enabled) {
        if (fastForwardButton != null) {
            fastForwardButton.setEnabled(enabled);
        }
    }

    /**
     * Adds or removes the Rewind button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setRewindButtonEnabled(boolean)}
     *
     * @param removed If the Rewind button should be removed [default: true]
     */
    @Override
    public void setRewindButtonRemoved(boolean removed) {
        if (rewindButton != null) {
            rewindButton.setVisibility(removed ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Adds or removes the FastForward button.  This will change the visibility
     * of the button, if you want to change the enabled/disabled images see {@link #setFastForwardButtonEnabled(boolean)}
     *
     * @param removed If the FastForward button should be removed [default: true]
     */
    @Override
    public void setFastForwardButtonRemoved(boolean removed) {
        if (fastForwardButton != null) {
            fastForwardButton.setVisibility(removed ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Retrieves the view references from the xml layout
     */
    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        progressBar = (ProgressBar) findViewById(R.id.exomedia_controls_video_progress);

        rewindButton = (ImageButton) findViewById(R.id.exomedia_controls_rewind_btn);
        fastForwardButton = (ImageButton) findViewById(R.id.exomedia_controls_fast_forward_btn);
        rippleIndicator = (ImageView) findViewById(R.id.exomedia_controls_leanback_ripple);
    }

    /**
     * Registers any internal listeners to perform the playback controls,
     * such as play/pause, next, and previous
     */
    @Override
    protected void registerListeners() {
        super.registerListeners();
        rewindButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRewindClick();
            }
        });
        fastForwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onFastForwardClick();
            }
        });

        //Registers the buttons for focus changes in order to update the ripple selector
        previousButton.setOnFocusChangeListener(buttonFocusChangeListener);
        rewindButton.setOnFocusChangeListener(buttonFocusChangeListener);
        playPauseButton.setOnFocusChangeListener(buttonFocusChangeListener);
        fastForwardButton.setOnFocusChangeListener(buttonFocusChangeListener);
        nextButton.setOnFocusChangeListener(buttonFocusChangeListener);
    }

    /**
     * Updates the drawables used for the buttons to AppCompatTintDrawables
     */
    @Override
    protected void updateButtonDrawables() {
        super.updateButtonDrawables();

        defaultRewindDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_rewind_white, R.color.exomedia_default_controls_button_selector);
        rewindButton.setImageDrawable(defaultRewindDrawable);

        defaultFastForwardDrawable = EMResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_fast_forward_white, R.color.exomedia_default_controls_button_selector);
        fastForwardButton.setImageDrawable(defaultFastForwardDrawable);
    }

    @Override
    protected void animateVisibility(boolean toVisible) {
        if (isVisible == toVisible) {
            return;
        }

        //TODO: make sure these are correct... (views exist, and animation looks ok)
        textContainer.startAnimation(new BottomViewHideShowAnimation(textContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        controlsContainer.startAnimation(new BottomViewHideShowAnimation(controlsContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));

        isVisible = toVisible;
        onVisibilityChanged();
    }

    /**
     * Performs the functionality to rewind the current video by
     * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
     */
    protected void onRewindClick() {
        if (buttonsListener == null || !buttonsListener.onRewindClicked()) {
            internalListener.onRewindClicked();
        }
    }

    /**
     * Performs the functionality to fast forward the current video by
     * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
     */
    protected void onFastForwardClick() {
        if (buttonsListener == null || !buttonsListener.onFastForwardClicked()) {
            internalListener.onFastForwardClicked();
        }
    }

    /**
     * Performs the functionality to inform any listeners that the video has been
     * seeked to the specified time.
     *
     * @param seekToTime The time to seek to in milliseconds
     */
    protected void performSeek(int seekToTime) {
        if (seekListener == null || !seekListener.onSeekEnded(seekToTime)) {
            internalListener.onSeekEnded(seekToTime);
        }
    }

    /**
     * Temporarily shows the default controls, hiding after the standard
     * delay.  If the {@link #videoView} is not playing then the controls
     * will not be hidden.
     */
    protected void showTemporary() {
        show();

        if (videoView != null && videoView.isPlaying()) {
            hideDelayed(DEFAULT_CONTROL_HIDE_DELAY);
        }
    }

    /**
     * Registers all selectable fields for key events in order
     * to correctly handle navigation.
     */
    protected void registerForInput() {
        RemoteKeyListener remoteKeyListener = new RemoteKeyListener();
        setOnKeyListener(remoteKeyListener);

        //Registers each button to make sure we catch the key events
        playPauseButton.setOnKeyListener(remoteKeyListener);
        previousButton.setOnKeyListener(remoteKeyListener);
        nextButton.setOnKeyListener(remoteKeyListener);
        rewindButton.setOnKeyListener(remoteKeyListener);
        fastForwardButton.setOnKeyListener(remoteKeyListener);
    }

    /**
     * Focuses the next visible view specified in the <code>view</code>
     *
     * @param view The view to find the next focus for
     */
    protected void focusNext(View view) {
        int nextId = view.getNextFocusRightId();
        if (nextId == NO_ID) {
            return;
        }

        View nextView = findViewById(nextId);
        if (nextView.getVisibility() != View.VISIBLE) {
            focusNext(nextView);
            return;
        }

        nextView.requestFocus();
        currentFocus = nextView;
        buttonFocusChangeListener.onFocusChange(nextView, true);
    }

    /**
     * Focuses the previous visible view specified in the <code>view</code>
     *
     * @param view The view to find the previous focus for
     */
    protected void focusPrevious(View view) {
        int previousId = view.getNextFocusLeftId();
        if (previousId == NO_ID) {
            return;
        }

        View previousView = findViewById(previousId);
        if (previousView.getVisibility() != View.VISIBLE) {
            focusPrevious(previousView);
            return;
        }

        previousView.requestFocus();
        currentFocus = previousView;
        buttonFocusChangeListener.onFocusChange(previousView, true);
    }

    /**
     * A listener to monitor the selected button and move the ripple
     * indicator when the focus shifts.
     */
    protected class ButtonFocusChangeListener implements OnFocusChangeListener {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (!hasFocus) {
                return;
            }

            //Performs the move animation
            int xDelta = getHorizontalDelta(view);
            rippleIndicator.startAnimation(new RippleTranslateAnimation(xDelta));
        }

        protected int getHorizontalDelta(View selectedView) {
            int[] position = new int[2];
            selectedView.getLocationOnScreen(position);

            int viewX = position[0];
            rippleIndicator.getLocationOnScreen(position);

            int newRippleX = viewX - ((rippleIndicator.getWidth() - selectedView.getWidth()) / 2);
            return newRippleX - position[0];
        }
    }

    /**
     * A listener to catch the key events so that we can correctly perform the
     * playback functionality and to hide/show the controls
     */
    protected class RemoteKeyListener implements OnKeyListener {
        /**
         * NOTE: the view is not always the currently focused view, thus the
         * {@link #currentFocus} variable
         */
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (isVisible) {
                        hideDelayed(0);
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    showTemporary();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    hideDelayed(0);
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    showTemporary();
                    focusPrevious(currentFocus);
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    showTemporary();
                    focusNext(currentFocus);
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    showTemporary();
                    currentFocus.callOnClick();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    onPlayPauseClick();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (videoView != null && !videoView.isPlaying()) {
                        videoView.start();
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    if (videoView != null && videoView.isPlaying()) {
                        videoView.pause();
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    onNextClick();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    onPreviousClick();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    onRewindClick();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    onFastForwardClick();
                    return true;
            }

            return false;
        }
    }

    /**
     * An animation for moving the ripple indicator to the correctly
     * focused view.
     */
    protected class RippleTranslateAnimation extends TranslateAnimation implements Animation.AnimationListener {
        protected static final long DURATION = 250;

        protected int xDelta;

        public RippleTranslateAnimation(int xDelta) {
            super(0, xDelta, 0, 0);

            this.xDelta = xDelta;
            setDuration(DURATION);
            setAnimationListener(this);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            //Purposefully left blank
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            rippleIndicator.setX(rippleIndicator.getX() + xDelta);
            rippleIndicator.clearAnimation();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            //Purposefully left blank
        }
    }

    protected class LeanbackInternalListener extends InternalListener {
        @Override
        public boolean onFastForwardClicked() {
            if (videoView == null) {
                return false;
            }

            int newPosition = videoView.getCurrentPosition() - FAST_FORWARD_REWIND_AMOUNT;
            if (newPosition < 0) {
                newPosition = 0;
            }

            performSeek(newPosition);
            return true;
        }

        @Override
        public boolean onRewindClicked() {
            if (videoView == null) {
                return false;
            }

            int newPosition = videoView.getCurrentPosition() + FAST_FORWARD_REWIND_AMOUNT;
            if (newPosition > progressBar.getMax()) {
                newPosition = progressBar.getMax();
            }

            performSeek(newPosition);
            return true;
        }
    }
}