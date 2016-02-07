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
import android.os.Build;
import android.support.annotation.IntRange;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;
import com.devbrackets.android.exomedia.ui.animation.BottomViewHideShowAnimation;
import com.devbrackets.android.exomedia.ui.animation.TopViewHideShowAnimation;

/**
 * Provides playback controls for the EMVideoView on Mobile
 * (Phone, Tablet, etc.) devices.
 */
public class VideoControlsMobile extends VideoControls {
    private SeekBar seekBar;
    private boolean pausedForSeek = false;
    private boolean userInteracting = false;

    public VideoControlsMobile(Context context) {
        super(context);
    }

    public VideoControlsMobile(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public VideoControlsMobile(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoControlsMobile(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.exomedia_default_controls_mobile;
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
        seekBar.setProgress((int) position);
    }

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    @Override
    public void setDuration(long duration) {
        if (duration != seekBar.getMax()) {
            endTime.setText(TimeFormatUtil.formatMs(duration));
            seekBar.setMax((int) duration);
        }
    }

    @Override
    public void updateProgress(@IntRange(from = 0) long position, @IntRange(from = 0) long duration, @IntRange(from = 0, to = 100) int bufferPercent) {
        if (!userInteracting) {
            seekBar.setSecondaryProgress((int) (seekBar.getMax() * ((float)bufferPercent / 100)));
            seekBar.setProgress((int) position);
            currentTime.setText(TimeFormatUtil.formatMs(position));
        }
    }

    /**
     * Retrieves the view references from the xml layout
     */
    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        seekBar = (SeekBar) findViewById(R.id.exomedia_controls_video_seek);
    }

    /**
     * Registers any internal listeners to perform the playback controls,
     * such as play/pause, next, and previous
     */
    @Override
    protected void registerListeners() {
        super.registerListeners();
        seekBar.setOnSeekBarChangeListener(new SeekBarChanged());
    }

    /**
     * After the specified delay the view will be hidden.  If the user is interacting
     * with the controls then we wait until after they are done to start the delay.
     *
     * @param delay The delay in milliseconds to wait to start the hide animation
     */
    @Override
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

    @Override
    protected void animateVisibility(boolean toVisible) {
        if (isVisible == toVisible) {
            return;
        }

        textContainer.startAnimation(new TopViewHideShowAnimation(textContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        controlsContainer.startAnimation(new BottomViewHideShowAnimation(controlsContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));

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