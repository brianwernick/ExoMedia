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
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;

/**
 * Provides playback controls for the EMVideoView on TV devices.
 */
public class DefaultControlsLeanback extends DefaultControls {
    private ProgressBar progressBar;
    private LinearLayout controlsContainer;

    private ImageButton fastForwardButton;
    private ImageButton rewindButton;

    private Drawable defaultRewindDrawable;
    private Drawable defaultFastForwardDrawable;

    public DefaultControlsLeanback(Context context) {
        super(context);
    }

    public DefaultControlsLeanback(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DefaultControlsLeanback(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DefaultControlsLeanback(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.exomedia_video_controls_overlay_leanback;
    }

    /**
     * Used to update the control view visibilities to indicate that the video
     * is loading.  This is different from using {@link #loadCompleted()} and {@link #restartLoading()}
     * because those update additional information.
     *
     * @param isLoading True if loading progress should be shown
     */
    @Override
    public void setLoading(boolean isLoading) {
        super.setLoading(isLoading);
        controlsContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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

    /**
     * Performs the progress update on the current time field,
     * and the seek bar
     *
     * @param event The most recent progress
     */
    @Override
    public void setProgressEvent(EMMediaProgressEvent event) {
        if (!userInteracting) {
            progressBar.setSecondaryProgress((int) (progressBar.getMax() * event.getBufferPercentFloat()));
            progressBar.setProgress((int) event.getPosition());
            currentTime.setText(TimeFormatUtil.formatMs(event.getPosition()));
        }
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
     * </p>
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
     * </p>
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

    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        progressBar = (ProgressBar) findViewById(R.id.exomedia_controls_video_progress);

        rewindButton = (ImageButton)findViewById(R.id.exomedia_controls_rewind_btn);
        fastForwardButton = (ImageButton) findViewById(R.id.exomedia_controls_fast_forward_btn);
        controlsContainer = (LinearLayout) findViewById(R.id.exomedia_controls_interactive_container);
    }

    @Override
    protected void registerClickListeners() {
        super.registerClickListeners();
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
    }

    /**
     * Updates the drawables used for the buttons to AppCompatTintDrawables
     */
    @Override
    protected void updateButtonDrawables() {
        defaultRewindDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_rewind_white));
        DrawableCompat.setTintList(defaultRewindDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));
        rewindButton.setImageDrawable(defaultRewindDrawable);

        defaultFastForwardDrawable = DrawableCompat.wrap(getDrawable(R.drawable.exomedia_ic_fast_forward_white));
        DrawableCompat.setTintList(defaultFastForwardDrawable, getResources().getColorStateList(R.color.exomedia_default_controls_button_selector));
        fastForwardButton.setImageDrawable(defaultFastForwardDrawable);
    }

    private void onRewindClick() {
        //TODO: ?
    }

    private void onFastForwardClick() {
        //TODO: ?
    }
}