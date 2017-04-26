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
import android.support.annotation.IntRange;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.ui.animation.BottomViewHideShowAnimation;
import com.devbrackets.android.exomedia.util.ResourceUtil;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;

/**
 * Provides playback controls for the {@link VideoView} on TV devices.
 */
@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoControlsLeanback extends VideoControls {
    protected static final int FAST_FORWARD_REWIND_AMOUNT = 10000; //10 seconds

    protected ProgressBar progressBar;

    protected ImageView rippleIndicator;
    protected ViewGroup controlsParent;

    protected ImageButton fastForwardButton;
    protected ImageButton rewindButton;

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
        setFocusable(true);
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

    @Override
    public void setPosition(long position) {
        currentTimeTextView.setText(TimeFormatUtil.formatMs(position));
        progressBar.setProgress((int) position);
    }

    @Override
    public void setDuration(long duration) {
        if (duration != progressBar.getMax()) {
            endTimeTextView.setText(TimeFormatUtil.formatMs(duration));
            progressBar.setMax((int) duration);
        }
    }

    @Override
    public void updateProgress(@IntRange(from = 0) long position, @IntRange(from = 0) long duration, @IntRange(from = 0, to = 100) int bufferPercent) {
        progressBar.setSecondaryProgress((int) (progressBar.getMax() * ((float)bufferPercent / 100)));
        progressBar.setProgress((int) position);
        currentTimeTextView.setText(TimeFormatUtil.formatMs(position));
    }

    @Override
    public void setRewindDrawable(Drawable drawable) {
        if (rewindButton != null) {
            rewindButton.setImageDrawable(drawable);
        }
    }

    @Override
    public void setFastForwardDrawable(Drawable drawable) {
        if (fastForwardButton != null) {
            fastForwardButton.setImageDrawable(drawable);
        }
    }

    @Override
    public void setRewindButtonEnabled(boolean enabled) {
        if (rewindButton != null) {
            rewindButton.setEnabled(enabled);
            enabledViews.put(R.id.exomedia_controls_rewind_btn, enabled);
        }
    }

    @Override
    public void setFastForwardButtonEnabled(boolean enabled) {
        if (fastForwardButton != null) {
            fastForwardButton.setEnabled(enabled);
            enabledViews.put(R.id.exomedia_controls_fast_forward_btn, enabled);
        }
    }

    @Override
    public void setRewindButtonRemoved(boolean removed) {
        if (rewindButton != null) {
            rewindButton.setVisibility(removed ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void setFastForwardButtonRemoved(boolean removed) {
        if (fastForwardButton != null) {
            fastForwardButton.setVisibility(removed ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        progressBar = (ProgressBar) findViewById(R.id.exomedia_controls_video_progress);

        rewindButton = (ImageButton) findViewById(R.id.exomedia_controls_rewind_btn);
        fastForwardButton = (ImageButton) findViewById(R.id.exomedia_controls_fast_forward_btn);
        rippleIndicator = (ImageView) findViewById(R.id.exomedia_controls_leanback_ripple);
        controlsParent = (ViewGroup) findViewById(R.id.exomedia_controls_parent);
    }

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

    @Override
    protected void updateButtonDrawables() {
        super.updateButtonDrawables();

        Drawable rewindDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_rewind_white, R.color.exomedia_default_controls_button_selector);
        rewindButton.setImageDrawable(rewindDrawable);

        Drawable fastForwardDrawable = ResourceUtil.tintList(getContext(), R.drawable.exomedia_ic_fast_forward_white, R.color.exomedia_default_controls_button_selector);
        fastForwardButton.setImageDrawable(fastForwardDrawable);
    }

    @Override
    protected void animateVisibility(boolean toVisible) {
        if (isVisible == toVisible) {
            return;
        }

        if (!isLoading) {
            controlsParent.startAnimation(new BottomViewHideShowAnimation(controlsParent, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        }

        isVisible = toVisible;
        onVisibilityChanged();
    }

    @Override
    protected void updateTextContainerVisibility() {
        if (!isVisible) {
            return;
        }

        boolean emptyText = isTextContainerEmpty();
        if (hideEmptyTextContainer && emptyText && textContainer.getVisibility() == VISIBLE) {
            textContainer.clearAnimation();
            textContainer.startAnimation(new BottomViewHideShowAnimation(textContainer, false, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        } else if ((!hideEmptyTextContainer || !emptyText) && textContainer.getVisibility() != VISIBLE) {
            textContainer.clearAnimation();
            textContainer.startAnimation(new BottomViewHideShowAnimation(textContainer, true, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        }
    }

    @Override
    public void showLoading(boolean initialLoad) {
        if (isLoading) {
            return;
        }

        isLoading = true;
        controlsContainer.setVisibility(View.GONE);
        rippleIndicator.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.VISIBLE);

        show();
    }

    @Override
    public void finishLoading() {
        if (!isLoading) {
            return;
        }

        isLoading = false;
        controlsContainer.setVisibility(View.VISIBLE);
        rippleIndicator.setVisibility(View.VISIBLE);
        loadingProgressBar.setVisibility(View.GONE);

        updatePlaybackState(videoView != null && videoView.isPlaying());
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
    protected void performSeek(long seekToTime) {
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
            hideDelayed();
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
                    if (isVisible && canViewHide && !isLoading) {
                        hide();
                        return true;
                    } else if (controlsParent.getAnimation() != null) {
                        //This occurs if we are animating the hide or show of the controls
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    showTemporary();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    hide();
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

            long newPosition = videoView.getCurrentPosition() + FAST_FORWARD_REWIND_AMOUNT;
            if (newPosition > progressBar.getMax()) {
                newPosition = progressBar.getMax();
            }

            performSeek(newPosition);
            return true;
        }

        @Override
        public boolean onRewindClicked() {
            if (videoView == null) {
                return false;
            }

            long newPosition = videoView.getCurrentPosition() - FAST_FORWARD_REWIND_AMOUNT;
            if (newPosition < 0) {
                newPosition = 0;
            }

            performSeek(newPosition);
            return true;
        }
    }
}