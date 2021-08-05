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
package com.devbrackets.android.exomediademo.ui.subtitle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.graphics.Paint.Style
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout.Alignment
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.util.Log
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.util.Util

/**
 * Paints subtitle [Cue]s.
 */
internal class SubtitlePainter(context: Context) {

    /**
     * Temporary rectangle used for computing line bounds.
     */
    private val lineBounds = RectF()

    // Styled dimensions.
    private val cornerRadius: Float
    private val outlineWidth: Float
    private val shadowRadius: Float
    private val shadowOffset: Float
    private val spacingMult: Float
    private val spacingAdd: Float

    private val textPaint: TextPaint
    private val paint: Paint

    // Previous input variables.
    private var cueText: CharSequence? = null
    private var cueTextAlignment: Alignment? = null
    private var cueBitmap: Bitmap? = null
    private var cueLine: Float = 0.toFloat()
    @Cue.LineType
    private var cueLineType: Int = 0
    @Cue.AnchorType
    private var cueLineAnchor: Int = 0
    private var cuePosition: Float = 0.toFloat()
    @Cue.AnchorType
    private var cuePositionAnchor: Int = 0
    private var cueSize: Float = 0.toFloat()
    private var cueBitmapHeight: Float = 0.toFloat()
    private var applyEmbeddedStyles: Boolean = false
    private var applyEmbeddedFontSizes: Boolean = false
    private var foregroundColor: Int = 0
    private var backgroundColor: Int = 0
    private var windowColor: Int = 0
    private var edgeColor: Int = 0
    @CaptionStyleCompat.EdgeType
    private var edgeType: Int = 0
    private var defaultTextSizePx: Float = 0.toFloat()
    private var cueTextSizePx: Float = 0.toFloat()
    private var bottomPaddingFraction: Float = 0.toFloat()
    private var parentLeft: Int = 0
    private var parentTop: Int = 0
    private var parentRight: Int = 0
    private var parentBottom: Int = 0

    // Derived drawing variables.
    private var textLayout: StaticLayout? = null
    private var textLeft: Int = 0
    private var textTop: Int = 0
    private var textPaddingX: Int = 0
    private var bitmapRect: Rect? = null

    init {
        val viewAttr = intArrayOf(android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier)
        val styledAttributes = context.obtainStyledAttributes(null, viewAttr, 0, 0)
        spacingAdd = styledAttributes.getDimensionPixelSize(0, 0).toFloat()
        @SuppressLint("ResourceType")
        spacingMult = styledAttributes.getFloat(1, 1f)
        styledAttributes.recycle()

        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val twoDpInPx = Math.round(2f * displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
        cornerRadius = twoDpInPx.toFloat()
        outlineWidth = twoDpInPx.toFloat()
        shadowRadius = twoDpInPx.toFloat()
        shadowOffset = twoDpInPx.toFloat()

        textPaint = TextPaint()
        textPaint.isAntiAlias = true
        textPaint.isSubpixelText = true

        paint = Paint()
        paint.isAntiAlias = true
        paint.style = Style.FILL
    }

    /**
     * Draws the provided [Cue] into a canvas with the specified styling.
     *
     *
     * A call to this method is able to use cached results of calculations made during the previous
     * call, and so an instance of this class is able to optimize repeated calls to this method in
     * which the same parameters are passed.
     *
     * @param cue The cue to draw.
     * @param applyEmbeddedStyles Whether styling embedded within the cue should be applied.
     * @param applyEmbeddedFontSizes If `applyEmbeddedStyles` is true, defines whether font
     * sizes embedded within the cue should be applied. Otherwise, it is ignored.
     * @param style The style to use when drawing the cue text.
     * @param defaultTextSizePx The default text size to use when drawing the text, in pixels.
     * @param cueTextSizePx The embedded text size of this cue, in pixels.
     * @param bottomPaddingFraction The bottom padding fraction to apply when [Cue.line] is
     * [Cue.DIMEN_UNSET], as a fraction of the viewport height
     * @param canvas The canvas into which to draw.
     * @param cueBoxLeft The left position of the enclosing cue box.
     * @param cueBoxTop The top position of the enclosing cue box.
     * @param cueBoxRight The right position of the enclosing cue box.
     * @param cueBoxBottom The bottom position of the enclosing cue box.
     */
    fun draw(
        cue: Cue,
        applyEmbeddedStyles: Boolean,
        applyEmbeddedFontSizes: Boolean,
        style: CaptionStyleCompat,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        canvas: Canvas,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ) {
        val isTextCue = cue.bitmap == null
        var windowColor = Color.BLACK
        if (isTextCue) {
            if (TextUtils.isEmpty(cue.text)) {
                // Nothing to draw.
                return
            }
            windowColor = if (cue.windowColorSet && applyEmbeddedStyles)
                cue.windowColor
            else
                style.windowColor
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
            && Util.areEqual(this.textPaint.typeface, style.typeface)
            && this.defaultTextSizePx == defaultTextSizePx
            && this.cueTextSizePx == cueTextSizePx
            && this.bottomPaddingFraction == bottomPaddingFraction
            && this.parentLeft == cueBoxLeft
            && this.parentTop == cueBoxTop
            && this.parentRight == cueBoxRight
            && this.parentBottom == cueBoxBottom
        ) {
            // We can use the cached layout.
            drawLayout(canvas, isTextCue)
            return
        }

        this.cueText = cue.text
        this.cueTextAlignment = cue.textAlignment
        this.cueBitmap = cue.bitmap
        this.cueLine = cue.line
        this.cueLineType = cue.lineType
        this.cueLineAnchor = cue.lineAnchor
        this.cuePosition = cue.position
        this.cuePositionAnchor = cue.positionAnchor
        this.cueSize = cue.size
        this.cueBitmapHeight = cue.bitmapHeight
        this.applyEmbeddedStyles = applyEmbeddedStyles
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes
        this.foregroundColor = style.foregroundColor
        this.backgroundColor = style.backgroundColor
        this.windowColor = windowColor
        this.edgeType = style.edgeType
        this.edgeColor = style.edgeColor
        this.textPaint.typeface = style.typeface
        this.defaultTextSizePx = defaultTextSizePx
        this.cueTextSizePx = cueTextSizePx
        this.bottomPaddingFraction = bottomPaddingFraction
        this.parentLeft = cueBoxLeft
        this.parentTop = cueBoxTop
        this.parentRight = cueBoxRight
        this.parentBottom = cueBoxBottom

        if (isTextCue) {
            setupTextLayout()
        } else {
            setupBitmapLayout()
        }
        drawLayout(canvas, isTextCue)
    }

    private fun setupTextLayout() {
        val parentWidth = parentRight - parentLeft
        val parentHeight = parentBottom - parentTop

        textPaint.textSize = defaultTextSizePx
        val textPaddingX = (defaultTextSizePx * INNER_PADDING_RATIO + 0.5f).toInt()

        var availableWidth = parentWidth - textPaddingX * 2
        if (cueSize != Cue.DIMEN_UNSET) {
            availableWidth = (availableWidth * cueSize).toInt()
        }
        if (availableWidth <= 0) {
            Log.w("SubtitlePainter", "Skipped drawing subtitle cue (insufficient space)")
            return
        }

        var cueText = this.cueText
        // Remove embedded styling or font size if requested.
        if (!applyEmbeddedStyles) {
            cueText = cueText?.toString() ?: ""// Equivalent to erasing all spans.
        } else if (!applyEmbeddedFontSizes) {
            val newCueText = SpannableStringBuilder(cueText)
            val cueLength = newCueText.length
            val absSpans = newCueText.getSpans(0, cueLength, AbsoluteSizeSpan::class.java)
            val relSpans = newCueText.getSpans(0, cueLength, RelativeSizeSpan::class.java)
            for (absSpan in absSpans) {
                newCueText.removeSpan(absSpan)
            }
            for (relSpan in relSpans) {
                newCueText.removeSpan(relSpan)
            }
            cueText = newCueText
        } else {
            // Apply embedded styles & font size.
            if (cueTextSizePx > 0) {
                // Use a SpannableStringBuilder encompassing the whole cue text to apply the default
                // cueTextSizePx.
                val newCueText = SpannableStringBuilder(cueText)
                newCueText.setSpan(
                    AbsoluteSizeSpan(cueTextSizePx.toInt()),
                    /* start= */ 0,
                    /* end= */ newCueText.length,
                    Spanned.SPAN_PRIORITY
                )
                cueText = newCueText
            }
        }

        val textAlignment = if (cueTextAlignment == null) Alignment.ALIGN_CENTER else cueTextAlignment
        textLayout = StaticLayout(
            cueText, textPaint, availableWidth, textAlignment, spacingMult,
            spacingAdd, true
        )
        val textHeight = textLayout?.height ?: 0
        var textWidth = 0
        val lineCount = textLayout?.lineCount ?: 0
        for (i in 0 until lineCount) {
            textWidth = Math.max(Math.ceil(textLayout?.getLineWidth(i)?.toDouble() ?: 0.0).toInt(), textWidth)
        }
        if (cueSize != Cue.DIMEN_UNSET && textWidth < availableWidth) {
            textWidth = availableWidth
        }
        textWidth += textPaddingX * 2

        var textLeft: Int
        val textRight: Int
        if (cuePosition != Cue.DIMEN_UNSET) {
            val anchorPosition = Math.round(parentWidth * cuePosition) + parentLeft
            textLeft = when (cuePositionAnchor) {
                Cue.ANCHOR_TYPE_END -> anchorPosition - textWidth
                Cue.ANCHOR_TYPE_MIDDLE -> (anchorPosition * 2 - textWidth) / 2
                else -> anchorPosition
            }
            textLeft = Math.max(textLeft, parentLeft)
            textRight = Math.min(textLeft + textWidth, parentRight)
        } else {
            textLeft = (parentWidth - textWidth) / 2
            textRight = textLeft + textWidth
        }

        textWidth = textRight - textLeft
        if (textWidth <= 0) {
            Log.w("SubtitlePainter", "Skipped drawing subtitle cue (invalid horizontal positioning)")
            return
        }

        var textTop: Int
        if (cueLine != Cue.DIMEN_UNSET) {
            val anchorPosition: Int
            anchorPosition = if (cueLineType == Cue.LINE_TYPE_FRACTION) {
                Math.round(parentHeight * cueLine) + parentTop
            } else {
                // cueLineType == Cue.LINE_TYPE_NUMBER
                val firstLineHeight = (textLayout?.getLineBottom(0) ?: 0) - (textLayout?.getLineTop(0) ?: 0)
                if (cueLine >= 0) {
                    Math.round(cueLine * firstLineHeight) + parentTop
                } else {
                    Math.round((cueLine + 1) * firstLineHeight) + parentBottom
                }
            }
            textTop = when (cueLineAnchor) {
                Cue.ANCHOR_TYPE_END -> anchorPosition - textHeight
                Cue.ANCHOR_TYPE_MIDDLE -> (anchorPosition * 2 - textHeight) / 2
                else -> anchorPosition
            }
            if (textTop + textHeight > parentBottom) {
                textTop = parentBottom - textHeight
            } else if (textTop < parentTop) {
                textTop = parentTop
            }
        } else {
            textTop = parentBottom - textHeight - (parentHeight * bottomPaddingFraction).toInt()
        }

        // Update the derived drawing variables.
        this.textLayout = StaticLayout(
            cueText, textPaint, textWidth, textAlignment, spacingMult,
            spacingAdd, true
        )
        this.textLeft = textLeft
        this.textTop = textTop
        this.textPaddingX = textPaddingX
    }

    private fun setupBitmapLayout() {
        val parentWidth = parentRight - parentLeft
        val parentHeight = parentBottom - parentTop
        val anchorX = parentLeft + parentWidth * cuePosition
        val anchorY = parentTop + parentHeight * cueLine
        val height = if (cueBitmapHeight != Cue.DIMEN_UNSET) {
            Math.round(parentHeight * cueBitmapHeight)
        } else {
            val height = cueBitmap?.height ?: 0
            val width = cueBitmap?.width ?: 1
            Math.round(width * (height.toFloat() / width))
        }
        val width = Math.round(parentWidth * cueSize)
        val x = Math.round(
            when (cueLineAnchor) {
                Cue.ANCHOR_TYPE_END -> anchorX - width
                Cue.ANCHOR_TYPE_MIDDLE -> anchorX - width / 2
                else -> anchorX
            }
        )
        val y = Math.round(
            when (cuePositionAnchor) {
                Cue.ANCHOR_TYPE_END -> anchorY - height
                Cue.ANCHOR_TYPE_MIDDLE -> anchorY - height / 2
                else -> anchorY
            }
        )
        bitmapRect = Rect(x, y, x + width, y + height)
    }

    private fun drawLayout(canvas: Canvas, isTextCue: Boolean) {
        if (isTextCue) {
            drawTextLayout(canvas)
        } else {
            drawBitmapLayout(canvas)
        }
    }

    private fun drawTextLayout(canvas: Canvas) {
        val layout = textLayout ?: return

        val saveCount = canvas.save()
        canvas.translate(textLeft.toFloat(), textTop.toFloat())

        if (Color.alpha(windowColor) > 0) {
            paint.color = windowColor
            canvas.drawRect(
                (-textPaddingX).toFloat(), 0f, (layout.width + textPaddingX).toFloat(), layout.height.toFloat(),
                paint
            )
        }

        if (Color.alpha(backgroundColor) > 0) {
            paint.color = backgroundColor
            var previousBottom = layout.getLineTop(0).toFloat()
            val lineCount = layout.lineCount
            for (i in 0 until lineCount) {
                val lineTextBoundLeft = layout.getLineLeft(i)
                val lineTextBoundRight = layout.getLineRight(i)
                lineBounds.left = lineTextBoundLeft - textPaddingX
                lineBounds.right = lineTextBoundRight + textPaddingX
                lineBounds.top = previousBottom
                lineBounds.bottom = layout.getLineBottom(i).toFloat()
                previousBottom = lineBounds.bottom
                val lineTextWidth = lineTextBoundRight - lineTextBoundLeft
                if (lineTextWidth > 0) {
                    // Do not draw a line's background color if it has no text.
                    // For some reason, calculating the width manually is more reliable than
                    // layout.getLineWidth().
                    // Sometimes, lineTextBoundRight == lineTextBoundLeft, and layout.getLineWidth() still
                    // returns non-zero value.
                    canvas.drawRoundRect(lineBounds, cornerRadius, cornerRadius, paint)
                }
            }
        }

        if (edgeType == CaptionStyleCompat.EDGE_TYPE_OUTLINE) {
            textPaint.strokeJoin = Join.ROUND
            textPaint.strokeWidth = outlineWidth
            textPaint.color = edgeColor
            textPaint.style = Style.FILL_AND_STROKE
            layout.draw(canvas)
        } else if (edgeType == CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW) {
            textPaint.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, edgeColor)
        } else if (edgeType == CaptionStyleCompat.EDGE_TYPE_RAISED || edgeType == CaptionStyleCompat.EDGE_TYPE_DEPRESSED) {
            val raised = edgeType == CaptionStyleCompat.EDGE_TYPE_RAISED
            val colorUp = if (raised) Color.WHITE else edgeColor
            val colorDown = if (raised) edgeColor else Color.WHITE
            val offset = shadowRadius / 2f
            textPaint.color = foregroundColor
            textPaint.style = Style.FILL
            textPaint.setShadowLayer(shadowRadius, -offset, -offset, colorUp)
            layout.draw(canvas)
            textPaint.setShadowLayer(shadowRadius, offset, offset, colorDown)
        }

        textPaint.color = foregroundColor
        textPaint.style = Style.FILL
        layout.draw(canvas)
        textPaint.setShadowLayer(0f, 0f, 0f, 0)

        canvas.restoreToCount(saveCount)
    }

    private fun drawBitmapLayout(canvas: Canvas) {
        val bitmap = cueBitmap ?: return
        val rect = bitmapRect ?: return
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    companion object {

        /**
         * Ratio of inner padding to font size.
         */
        private const val INNER_PADDING_RATIO = 0.125f

        /**
         * This method is used instead of [TextUtils.equals] because the
         * latter only checks the text of each sequence, and does not check for equality of styling that
         * may be embedded within the [CharSequence]s.
         */
        private fun areCharSequencesEqual(first: CharSequence?, second: CharSequence?): Boolean {
            // Some CharSequence implementations don't perform a cheap referential equality check in their
            // equals methods, so we perform one explicitly here.
            @Suppress("SuspiciousEqualsCombination")
            return first === second || first != null && first == second
        }
    }

}
