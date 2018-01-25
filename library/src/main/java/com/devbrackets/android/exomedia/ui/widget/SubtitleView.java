package com.devbrackets.android.exomedia.ui.widget;

/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A view for displaying subtitle {@link Cue}s.
 */
public final class SubtitleView extends View implements TextRenderer.Output {

    /**
     * The default fractional text size.
     *
     * @see #setFractionalTextSize(float, boolean)
     */
    public static final float DEFAULT_TEXT_SIZE_FRACTION = 0.0533f;

    /**
     * The default bottom padding to apply when {@link Cue#line} is {@link Cue#DIMEN_UNSET}, as a
     * fraction of the viewport height.
     *
     * @see #setBottomPaddingFraction(float)
     */
    public static final float DEFAULT_BOTTOM_PADDING_FRACTION = 0.08f;

    private static final int FRACTIONAL = 0;
    private static final int FRACTIONAL_IGNORE_PADDING = 1;
    private static final int ABSOLUTE = 2;

    private final List<SubtitlePainter> painters;

    private List<Cue> cues;
    private int textSizeType;
    private float textSize;
    private boolean applyEmbeddedStyles;
    private boolean applyEmbeddedFontSizes;
    private CaptionStyleCompat style;
    private float bottomPaddingFraction;

    public SubtitleView(Context context) {
        this(context, null);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        painters = new ArrayList<>();
        textSizeType = FRACTIONAL;
        textSize = DEFAULT_TEXT_SIZE_FRACTION;
        applyEmbeddedStyles = true;
        applyEmbeddedFontSizes = true;
        style = CaptionStyleCompat.DEFAULT;
        bottomPaddingFraction = DEFAULT_BOTTOM_PADDING_FRACTION;
    }

    @Override
    public void onCues(List<Cue> cues) {
        setCues(cues);
    }

    /**
     * Sets the cues to be displayed by the view.
     *
     * @param cues The cues to display.
     */
    public void setCues(List<Cue> cues) {
        if (this.cues == cues) {
            return;
        }
        this.cues = cues;
        // Ensure we have sufficient painters.
        int cueCount = (cues == null) ? 0 : cues.size();
        while (painters.size() < cueCount) {
            painters.add(new SubtitlePainter(getContext()));
        }
        // Invalidate to trigger drawing.
        invalidate();
    }

    /**
     * Set the text size to a given unit and value.
     * <p>
     * See {@link TypedValue} for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     */
    public void setFixedTextSize(int unit, float size) {
        Context context = getContext();
        Resources resources;
        if (context == null) {
            resources = Resources.getSystem();
        } else {
            resources = context.getResources();
        }
        setTextSize(ABSOLUTE, TypedValue.applyDimension(unit, size, resources.getDisplayMetrics()));
    }

    /**
     * Sets the text size to one derived from {@link CaptioningManager#getFontScale()}, or to a
     * default size before API level 19.
     */
    public void setUserDefaultTextSize() {
        float fontScale = Util.SDK_INT >= 19 && !isInEditMode() ? getUserCaptionFontScaleV19() : 1f;
        setFractionalTextSize(DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }

    /**
     * Sets the text size to be a fraction of the view's remaining height after its top and bottom
     * padding have been subtracted.
     * <p>
     * Equivalent to {@code #setFractionalTextSize(fractionOfHeight, false)}.
     *
     * @param fractionOfHeight A fraction between 0 and 1.
     */
    public void setFractionalTextSize(float fractionOfHeight) {
        setFractionalTextSize(fractionOfHeight, false);
    }

    /**
     * Sets the text size to be a fraction of the height of this view.
     *
     * @param fractionOfHeight A fraction between 0 and 1.
     * @param ignorePadding    Set to true if {@code fractionOfHeight} should be interpreted as a
     *                         fraction of this view's height ignoring any top and bottom padding. Set to false if
     *                         {@code fractionOfHeight} should be interpreted as a fraction of this view's remaining
     *                         height after the top and bottom padding has been subtracted.
     */
    public void setFractionalTextSize(float fractionOfHeight, boolean ignorePadding) {
        setTextSize(ignorePadding ? FRACTIONAL_IGNORE_PADDING : FRACTIONAL, fractionOfHeight);
    }

    private void setTextSize(int textSizeType, float textSize) {
        if (this.textSizeType == textSizeType && this.textSize == textSize) {
            return;
        }
        this.textSizeType = textSizeType;
        this.textSize = textSize;
        // Invalidate to trigger drawing.
        invalidate();
    }

    /**
     * Sets whether styling embedded within the cues should be applied. Enabled by default.
     * Overrides any setting made with {@link SubtitleView#setApplyEmbeddedFontSizes}.
     *
     * @param applyEmbeddedStyles Whether styling embedded within the cues should be applied.
     */
    public void setApplyEmbeddedStyles(boolean applyEmbeddedStyles) {
        if (this.applyEmbeddedStyles == applyEmbeddedStyles
                && this.applyEmbeddedFontSizes == applyEmbeddedStyles) {
            return;
        }
        this.applyEmbeddedStyles = applyEmbeddedStyles;
        this.applyEmbeddedFontSizes = applyEmbeddedStyles;
        // Invalidate to trigger drawing.
        invalidate();
    }

    /**
     * Sets whether font sizes embedded within the cues should be applied. Enabled by default.
     * Only takes effect if {@link SubtitleView#setApplyEmbeddedStyles} is set to true.
     *
     * @param applyEmbeddedFontSizes Whether font sizes embedded within the cues should be applied.
     */
    public void setApplyEmbeddedFontSizes(boolean applyEmbeddedFontSizes) {
        if (this.applyEmbeddedFontSizes == applyEmbeddedFontSizes) {
            return;
        }
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes;
        // Invalidate to trigger drawing.
        invalidate();
    }

    /**
     * Sets the caption style to be equivalent to the one returned by
     * {@link CaptioningManager#getUserStyle()}, or to a default style before API level 19.
     */
    public void setUserDefaultStyle() {
        setStyle(Util.SDK_INT >= 19 && !isInEditMode()
                ? getUserCaptionStyleV19() : CaptionStyleCompat.DEFAULT);
    }

    /**
     * Sets the caption style.
     *
     * @param style A style for the view.
     */
    public void setStyle(CaptionStyleCompat style) {
        if (this.style == style) {
            return;
        }
        this.style = style;
        // Invalidate to trigger drawing.
        invalidate();
    }

    /**
     * Sets the bottom padding fraction to apply when {@link Cue#line} is {@link Cue#DIMEN_UNSET},
     * as a fraction of the view's remaining height after its top and bottom padding have been
     * subtracted.
     * <p>
     * Note that this padding is applied in addition to any standard view padding.
     *
     * @param bottomPaddingFraction The bottom padding fraction.
     */
    public void setBottomPaddingFraction(float bottomPaddingFraction) {
        if (this.bottomPaddingFraction == bottomPaddingFraction) {
            return;
        }
        this.bottomPaddingFraction = bottomPaddingFraction;
        // Invalidate to trigger drawing.
        invalidate();
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        int cueCount = (cues == null) ? 0 : cues.size();
        int rawTop = getTop();
        int rawBottom = getBottom();

        // Calculate the bounds after padding is taken into account.
        int left = getLeft() + getPaddingLeft();
        int top = rawTop + getPaddingTop();
        int right = getRight() + getPaddingRight();
        int bottom = rawBottom - getPaddingBottom();
        if (bottom <= top || right <= left) {
            // No space to draw subtitles.
            return;
        }

        float textSizePx = textSizeType == ABSOLUTE ? textSize
                : textSize * (textSizeType == FRACTIONAL ? (bottom - top) : (rawBottom - rawTop));
        if (textSizePx <= 0) {
            // Text has no height.
            return;
        }

        for (int i = 0; i < cueCount; i++) {
            painters.get(i).draw(cues.get(i), applyEmbeddedStyles, applyEmbeddedFontSizes, style,
                    textSizePx, bottomPaddingFraction, canvas, left, top, right, bottom);
        }
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

}

