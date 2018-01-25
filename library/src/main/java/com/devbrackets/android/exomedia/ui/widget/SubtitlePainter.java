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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Util;

/**
 * Paints subtitle {@link Cue}s.
 */
/* package */ final class SubtitlePainter {

    private static final String TAG = "SubtitlePainter";

    /**
     * Ratio of inner padding to font size.
     */
    private static final float INNER_PADDING_RATIO = 0.125f;

    /**
     * Temporary rectangle used for computing line bounds.
     */
    private final RectF lineBounds = new RectF();

    // Styled dimensions.
    private final float cornerRadius;
    private final float outlineWidth;
    private final float shadowRadius;
    private final float shadowOffset;
    private final float spacingMult;
    private final float spacingAdd;

    private final TextPaint textPaint;
    private final Paint paint;

    // Previous input variables.
    private CharSequence cueText;
    private Alignment cueTextAlignment;
    private Bitmap cueBitmap;
    private float cueLine;
    @Cue.LineType
    private int cueLineType;
    @Cue.AnchorType
    private int cueLineAnchor;
    private float cuePosition;
    @Cue.AnchorType
    private int cuePositionAnchor;
    private float cueSize;
    private float cueBitmapHeight;
    private boolean applyEmbeddedStyles;
    private boolean applyEmbeddedFontSizes;
    private int foregroundColor;
    private int backgroundColor;
    private int windowColor;
    private int edgeColor;
    @CaptionStyleCompat.EdgeType
    private int edgeType;
    private float textSizePx;
    private float bottomPaddingFraction;
    private int parentLeft;
    private int parentTop;
    private int parentRight;
    private int parentBottom;

    // Derived drawing variables.
    private StaticLayout textLayout;
    private int textLeft;
    private int textTop;
    private int textPaddingX;
    private Rect bitmapRect;

    @SuppressWarnings("ResourceType")
    public SubtitlePainter(Context context) {
        int[] viewAttr = {android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier};
        TypedArray styledAttributes = context.obtainStyledAttributes(null, viewAttr, 0, 0);
        spacingAdd = styledAttributes.getDimensionPixelSize(0, 0);
        spacingMult = styledAttributes.getFloat(1, 1);
        styledAttributes.recycle();

        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int twoDpInPx = Math.round((2f * displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT);
        cornerRadius = twoDpInPx;
        outlineWidth = twoDpInPx;
        shadowRadius = twoDpInPx;
        shadowOffset = twoDpInPx;

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
    }

    /**
     * Draws the provided {@link Cue} into a canvas with the specified styling.
     * <p>
     * A call to this method is able to use cached results of calculations made during the previous
     * call, and so an instance of this class is able to optimize repeated calls to this method in
     * which the same parameters are passed.
     *
     * @param cue                    The cue to draw.
     * @param applyEmbeddedStyles    Whether styling embedded within the cue should be applied.
     * @param applyEmbeddedFontSizes If {@code applyEmbeddedStyles} is true, defines whether font
     *                               sizes embedded within the cue should be applied. Otherwise, it is ignored.
     * @param style                  The style to use when drawing the cue text.
     * @param textSizePx             The text size to use when drawing the cue text, in pixels.
     * @param bottomPaddingFraction  The bottom padding fraction to apply when {@link Cue#line} is
     *                               {@link Cue#DIMEN_UNSET}, as a fraction of the viewport height
     * @param canvas                 The canvas into which to draw.
     * @param cueBoxLeft             The left position of the enclosing cue box.
     * @param cueBoxTop              The top position of the enclosing cue box.
     * @param cueBoxRight            The right position of the enclosing cue box.
     * @param cueBoxBottom           The bottom position of the enclosing cue box.
     */
    public void draw(Cue cue, boolean applyEmbeddedStyles, boolean applyEmbeddedFontSizes,
                     CaptionStyleCompat style, float textSizePx, float bottomPaddingFraction, Canvas canvas,
                     int cueBoxLeft, int cueBoxTop, int cueBoxRight, int cueBoxBottom) {
        boolean isTextCue = cue.bitmap == null;
        int windowColor = Color.BLACK;
        if (isTextCue) {
            if (TextUtils.isEmpty(cue.text)) {
                // Nothing to draw.
                return;
            }
            windowColor = (cue.windowColorSet && applyEmbeddedStyles)
                    ? cue.windowColor : style.windowColor;
        }
        if (areCharSequencesEqual(this.cueText, cue.text)
                && Util.areEqual(this.cueTextAlignment, cue.textAlignment)
                && this.cueBitmap == cue.bitmap
                && this.cueLine == cue.line
                && this.cueLineType == cue.lineType
                && Util.areEqual(this.cueLineAnchor, cue.lineAnchor)
                && this.cuePosition == cue.position
                && Util.areEqual(this.cuePositionAnchor, cue.positionAnchor)
                && this.cueSize == cue.size
                && this.cueBitmapHeight == cue.bitmapHeight
                && this.applyEmbeddedStyles == applyEmbeddedStyles
                && this.applyEmbeddedFontSizes == applyEmbeddedFontSizes
                && this.foregroundColor == style.foregroundColor
                && this.backgroundColor == style.backgroundColor
                && this.windowColor == windowColor
                && this.edgeType == style.edgeType
                && this.edgeColor == style.edgeColor
                && Util.areEqual(this.textPaint.getTypeface(), style.typeface)
                && this.textSizePx == textSizePx
                && this.bottomPaddingFraction == bottomPaddingFraction
                && this.parentLeft == cueBoxLeft
                && this.parentTop == cueBoxTop
                && this.parentRight == cueBoxRight
                && this.parentBottom == cueBoxBottom) {
            // We can use the cached layout.
            drawLayout(canvas, isTextCue);
            return;
        }

        this.cueText = cue.text;
        this.cueTextAlignment = cue.textAlignment;
        this.cueBitmap = cue.bitmap;
        this.cueLine = cue.line;
        this.cueLineType = cue.lineType;
        this.cueLineAnchor = cue.lineAnchor;
        this.cuePosition = cue.position;
        this.cuePositionAnchor = cue.positionAnchor;
        this.cueSize = cue.size;
        this.cueBitmapHeight = cue.bitmapHeight;
        this.applyEmbeddedStyles = applyEmbeddedStyles;
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes;
        this.foregroundColor = style.foregroundColor;
        this.backgroundColor = style.backgroundColor;
        this.windowColor = windowColor;
        this.edgeType = style.edgeType;
        this.edgeColor = style.edgeColor;
        this.textPaint.setTypeface(style.typeface);
        this.textSizePx = textSizePx;
        this.bottomPaddingFraction = bottomPaddingFraction;
        this.parentLeft = cueBoxLeft;
        this.parentTop = cueBoxTop;
        this.parentRight = cueBoxRight;
        this.parentBottom = cueBoxBottom;

        if (isTextCue) {
            setupTextLayout();
        } else {
            setupBitmapLayout();
        }
        drawLayout(canvas, isTextCue);
    }

    private void setupTextLayout() {
        int parentWidth = parentRight - parentLeft;
        int parentHeight = parentBottom - parentTop;

        textPaint.setTextSize(textSizePx);
        int textPaddingX = (int) (textSizePx * INNER_PADDING_RATIO + 0.5f);

        int availableWidth = parentWidth - textPaddingX * 2;
        if (cueSize != Cue.DIMEN_UNSET) {
            availableWidth = (int) (availableWidth * cueSize);
        }
        if (availableWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (insufficient space)");
            return;
        }

        // Remove embedded styling or font size if requested.
        CharSequence cueText;
        if (applyEmbeddedFontSizes && applyEmbeddedStyles) {
            cueText = this.cueText;
        } else if (!applyEmbeddedStyles) {
            cueText = this.cueText.toString(); // Equivalent to erasing all spans.
        } else {
            SpannableStringBuilder newCueText = new SpannableStringBuilder(this.cueText);
            int cueLength = newCueText.length();
            AbsoluteSizeSpan[] absSpans = newCueText.getSpans(0, cueLength, AbsoluteSizeSpan.class);
            RelativeSizeSpan[] relSpans = newCueText.getSpans(0, cueLength, RelativeSizeSpan.class);
            for (AbsoluteSizeSpan absSpan : absSpans) {
                newCueText.removeSpan(absSpan);
            }
            for (RelativeSizeSpan relSpan : relSpans) {
                newCueText.removeSpan(relSpan);
            }
            cueText = newCueText;
        }

        Alignment textAlignment = cueTextAlignment == null ? Alignment.ALIGN_CENTER : cueTextAlignment;
        textLayout = new StaticLayout(cueText, textPaint, availableWidth, textAlignment, spacingMult,
                spacingAdd, true);
        int textHeight = textLayout.getHeight();
        int textWidth = 0;
        int lineCount = textLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            textWidth = Math.max((int) Math.ceil(textLayout.getLineWidth(i)), textWidth);
        }
        if (cueSize != Cue.DIMEN_UNSET && textWidth < availableWidth) {
            textWidth = availableWidth;
        }
        textWidth += textPaddingX * 2;

        int textLeft;
        int textRight;
        if (cuePosition != Cue.DIMEN_UNSET) {
            int anchorPosition = Math.round(parentWidth * cuePosition) + parentLeft;
            textLeft = cuePositionAnchor == Cue.ANCHOR_TYPE_END ? anchorPosition - textWidth
                    : cuePositionAnchor == Cue.ANCHOR_TYPE_MIDDLE ? (anchorPosition * 2 - textWidth) / 2
                    : anchorPosition;
            textLeft = Math.max(textLeft, parentLeft);
            textRight = Math.min(textLeft + textWidth, parentRight);
        } else {
            textLeft = (parentWidth - textWidth) / 2;
            textRight = textLeft + textWidth;
        }

        textWidth = textRight - textLeft;
        if (textWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (invalid horizontal positioning)");
            return;
        }

        int textTop;
        if (cueLine != Cue.DIMEN_UNSET) {
            int anchorPosition;
            if (cueLineType == Cue.LINE_TYPE_FRACTION) {
                anchorPosition = Math.round(parentHeight * cueLine) + parentTop;
            } else {
                // cueLineType == Cue.LINE_TYPE_NUMBER
                int firstLineHeight = textLayout.getLineBottom(0) - textLayout.getLineTop(0);
                if (cueLine >= 0) {
                    anchorPosition = Math.round(cueLine * firstLineHeight) + parentTop;
                } else {
                    anchorPosition = Math.round((cueLine + 1) * firstLineHeight) + parentBottom;
                }
            }
            textTop = cueLineAnchor == Cue.ANCHOR_TYPE_END ? anchorPosition - textHeight
                    : cueLineAnchor == Cue.ANCHOR_TYPE_MIDDLE ? (anchorPosition * 2 - textHeight) / 2
                    : anchorPosition;
            if (textTop + textHeight > parentBottom) {
                textTop = parentBottom - textHeight;
            } else if (textTop < parentTop) {
                textTop = parentTop;
            }
        } else {
            textTop = parentBottom - textHeight - (int) (parentHeight * bottomPaddingFraction);
        }

        // Update the derived drawing variables.
        this.textLayout = new StaticLayout(cueText, textPaint, textWidth, textAlignment, spacingMult,
                spacingAdd, true);
        this.textLeft = textLeft;
        this.textTop = textTop;
        this.textPaddingX = textPaddingX;
    }

    private void setupBitmapLayout() {
        int parentWidth = parentRight - parentLeft;
        int parentHeight = parentBottom - parentTop;
        float anchorX = parentLeft + (parentWidth * cuePosition);
        float anchorY = parentTop + (parentHeight * cueLine);
        int width = Math.round(parentWidth * cueSize);
        int height = cueBitmapHeight != Cue.DIMEN_UNSET ? Math.round(parentHeight * cueBitmapHeight)
                : Math.round(width * ((float) cueBitmap.getHeight() / cueBitmap.getWidth()));
        int x = Math.round(cueLineAnchor == Cue.ANCHOR_TYPE_END ? (anchorX - width)
                : cueLineAnchor == Cue.ANCHOR_TYPE_MIDDLE ? (anchorX - (width / 2)) : anchorX);
        int y = Math.round(cuePositionAnchor == Cue.ANCHOR_TYPE_END ? (anchorY - height)
                : cuePositionAnchor == Cue.ANCHOR_TYPE_MIDDLE ? (anchorY - (height / 2)) : anchorY);
        bitmapRect = new Rect(x, y, x + width, y + height);
    }

    private void drawLayout(Canvas canvas, boolean isTextCue) {
        if (isTextCue) {
            drawTextLayout(canvas);
        } else {
            drawBitmapLayout(canvas);
        }
    }

    private void drawTextLayout(Canvas canvas) {
        StaticLayout layout = textLayout;
        if (layout == null) {
            // Nothing to draw.
            return;
        }

        int saveCount = canvas.save();
        canvas.translate(textLeft, textTop);

        if (Color.alpha(windowColor) > 0) {
            paint.setColor(windowColor);
            canvas.drawRect(-textPaddingX, 0, layout.getWidth() + textPaddingX, layout.getHeight(),
                    paint);
        }

        if (Color.alpha(backgroundColor) > 0) {
            paint.setColor(backgroundColor);
            float previousBottom = layout.getLineTop(0);
            int lineCount = layout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                lineBounds.left = layout.getLineLeft(i) - textPaddingX;
                lineBounds.right = layout.getLineRight(i) + textPaddingX;
                lineBounds.top = previousBottom;
                lineBounds.bottom = layout.getLineBottom(i);
                previousBottom = lineBounds.bottom;
                canvas.drawRoundRect(lineBounds, cornerRadius, cornerRadius, paint);
            }
        }

        if (edgeType == CaptionStyleCompat.EDGE_TYPE_OUTLINE) {
            textPaint.setStrokeJoin(Join.ROUND);
            textPaint.setStrokeWidth(outlineWidth);
            textPaint.setColor(edgeColor);
            textPaint.setStyle(Style.FILL_AND_STROKE);
            layout.draw(canvas);
        } else if (edgeType == CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW) {
            textPaint.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, edgeColor);
        } else if (edgeType == CaptionStyleCompat.EDGE_TYPE_RAISED
                || edgeType == CaptionStyleCompat.EDGE_TYPE_DEPRESSED) {
            boolean raised = edgeType == CaptionStyleCompat.EDGE_TYPE_RAISED;
            int colorUp = raised ? Color.WHITE : edgeColor;
            int colorDown = raised ? edgeColor : Color.WHITE;
            float offset = shadowRadius / 2f;
            textPaint.setColor(foregroundColor);
            textPaint.setStyle(Style.FILL);
            textPaint.setShadowLayer(shadowRadius, -offset, -offset, colorUp);
            layout.draw(canvas);
            textPaint.setShadowLayer(shadowRadius, offset, offset, colorDown);
        }

        textPaint.setColor(foregroundColor);
        textPaint.setStyle(Style.FILL);
        layout.draw(canvas);
        textPaint.setShadowLayer(0, 0, 0, 0);

        canvas.restoreToCount(saveCount);
    }

    private void drawBitmapLayout(Canvas canvas) {
        canvas.drawBitmap(cueBitmap, null, bitmapRect, null);
    }

    /**
     * This method is used instead of {@link TextUtils#equals(CharSequence, CharSequence)} because the
     * latter only checks the text of each sequence, and does not check for equality of styling that
     * may be embedded within the {@link CharSequence}s.
     */
    private static boolean areCharSequencesEqual(CharSequence first, CharSequence second) {
        // Some CharSequence implementations don't perform a cheap referential equality check in their
        // equals methods, so we perform one explicitly here.
        return first == second || (first != null && first.equals(second));
    }

}
