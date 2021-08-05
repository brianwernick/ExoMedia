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

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.CaptioningManager
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.CaptionStyleCompat

@Suppress("MemberVisibilityCanBePrivate", "unused")
/**
 * A view for displaying subtitle [Cue]s.
 */
class SubtitleView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs), CaptionListener {

    private val painters = mutableListOf<SubtitlePainter>()

    private var cues: List<Cue>? = null
    @Cue.TextSizeType
    private var textSizeType = Cue.TEXT_SIZE_TYPE_FRACTIONAL
    private var textSize = DEFAULT_TEXT_SIZE_FRACTION
    private var applyEmbeddedStyles = true
    private var applyEmbeddedFontSizes = true
    private var style: CaptionStyleCompat = CaptionStyleCompat.DEFAULT
    private var bottomPaddingFraction = DEFAULT_BOTTOM_PADDING_FRACTION

    private val userCaptionFontScaleV19: Float
        get() {
            val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
            return captioningManager.fontScale
        }

    private val userCaptionStyleV19: CaptionStyleCompat
        get() {
            val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
            return CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        }

    override fun onCues(cues: List<Cue>) {
        setCues(cues)
    }

    /**
     * Sets the cues to be displayed by the view.
     *
     * @param cues The cues to display, or null to clear the cues.
     */
    fun setCues(cues: List<Cue>?) {
        if (this.cues === cues) {
            return
        }
        this.cues = cues
        // Ensure we have sufficient painters.
        val cueCount = cues?.size ?: 0
        while (painters.size < cueCount) {
            painters.add(SubtitlePainter(context))
        }
        // Invalidate to trigger drawing.
        invalidate()
    }

    /**
     * Set the text size to a given unit and value.
     *
     *
     * See [TypedValue] for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     */
    fun setFixedTextSize(unit: Int, size: Float) {
        val context = context
        val resources: Resources
        resources = if (context == null) {
            Resources.getSystem()
        } else {
            context.resources
        }
        setTextSize(
            Cue.TEXT_SIZE_TYPE_ABSOLUTE,
            TypedValue.applyDimension(unit, size, resources.displayMetrics)
        )
    }

    /**
     * Sets the text size to one derived from [CaptioningManager.getFontScale], or to a
     * default size before API level 19.
     */
    fun setUserDefaultTextSize() {
        val fontScale = if (!isInEditMode) userCaptionFontScaleV19 else 1f
        setFractionalTextSize(DEFAULT_TEXT_SIZE_FRACTION * fontScale)
    }

    /**
     * Sets the text size to be a fraction of the height of this view.
     *
     * @param fractionOfHeight A fraction between 0 and 1.
     * @param ignorePadding    Set to true if `fractionOfHeight` should be interpreted as a
     * fraction of this view's height ignoring any top and bottom padding. Set to false if
     * `fractionOfHeight` should be interpreted as a fraction of this view's remaining
     * height after the top and bottom padding has been subtracted.
     */
    @JvmOverloads
    fun setFractionalTextSize(fractionOfHeight: Float, ignorePadding: Boolean = false) {
        setTextSize(
            if (ignorePadding)
                Cue.TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING
            else
                Cue.TEXT_SIZE_TYPE_FRACTIONAL,
            fractionOfHeight
        )
    }

    private fun setTextSize(@Cue.TextSizeType textSizeType: Int, textSize: Float) {
        if (this.textSizeType == textSizeType && this.textSize == textSize) {
            return
        }
        this.textSizeType = textSizeType
        this.textSize = textSize
        // Invalidate to trigger drawing.
        invalidate()
    }

    /**
     * Sets whether styling embedded within the cues should be applied. Enabled by default.
     * Overrides any setting made with [SubtitleView.setApplyEmbeddedFontSizes].
     *
     * @param applyEmbeddedStyles Whether styling embedded within the cues should be applied.
     */
    fun setApplyEmbeddedStyles(applyEmbeddedStyles: Boolean) {
        if (this.applyEmbeddedStyles == applyEmbeddedStyles && this.applyEmbeddedFontSizes == applyEmbeddedStyles) {
            return
        }
        this.applyEmbeddedStyles = applyEmbeddedStyles
        this.applyEmbeddedFontSizes = applyEmbeddedStyles
        // Invalidate to trigger drawing.
        invalidate()
    }

    /**
     * Sets whether font sizes embedded within the cues should be applied. Enabled by default.
     * Only takes effect if [SubtitleView.setApplyEmbeddedStyles] is set to true.
     *
     * @param applyEmbeddedFontSizes Whether font sizes embedded within the cues should be applied.
     */
    fun setApplyEmbeddedFontSizes(applyEmbeddedFontSizes: Boolean) {
        if (this.applyEmbeddedFontSizes == applyEmbeddedFontSizes) {
            return
        }
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes
        // Invalidate to trigger drawing.
        invalidate()
    }

    /**
     * Sets the caption style to be equivalent to the one returned by
     * [CaptioningManager.getUserStyle], or to a default style before API level 19.
     */
    fun setUserDefaultStyle() {
        setStyle(
            if (!isInEditMode)
                userCaptionStyleV19
            else
                CaptionStyleCompat.DEFAULT
        )
    }

    /**
     * Sets the caption style.
     *
     * @param style A style for the view.
     */
    fun setStyle(style: CaptionStyleCompat) {
        if (this.style == style) {
            return
        }
        this.style = style
        // Invalidate to trigger drawing.
        invalidate()
    }

    /**
     * Sets the bottom padding fraction to apply when [Cue.line] is [Cue.DIMEN_UNSET],
     * as a fraction of the view's remaining height after its top and bottom padding have been
     * subtracted.
     *
     *
     * Note that this padding is applied in addition to any standard view padding.
     *
     * @param bottomPaddingFraction The bottom padding fraction.
     */
    fun setBottomPaddingFraction(bottomPaddingFraction: Float) {
        if (this.bottomPaddingFraction == bottomPaddingFraction) {
            return
        }
        this.bottomPaddingFraction = bottomPaddingFraction
        // Invalidate to trigger drawing.
        invalidate()
    }

    public override fun dispatchDraw(canvas: Canvas) {
        val cueCount = if (cues == null) 0 else cues!!.size
        val rawTop = top
        val rawBottom = bottom

        // Calculate the bounds after padding is taken into account.
        val left = left + paddingLeft
        val top = rawTop + paddingTop
        val right = right - paddingRight
        val bottom = rawBottom - paddingBottom
        if (bottom <= top || right <= left) {
            // No space to draw subtitles.
            return
        }
        val rawViewHeight = rawBottom - rawTop
        val viewHeightMinusPadding = bottom - top

        val defaultViewTextSizePx = resolveTextSize(textSizeType, textSize, rawViewHeight, viewHeightMinusPadding)
        if (defaultViewTextSizePx <= 0) {
            // Text has no height.
            return
        }

        for (i in 0 until cueCount) {
            val cue = cues?.get(i) ?: break
            val cueTextSizePx = resolveCueTextSize(cue, rawViewHeight, viewHeightMinusPadding)
            val painter = painters[i]
            painter.draw(
                cue,
                applyEmbeddedStyles,
                applyEmbeddedFontSizes,
                style,
                defaultViewTextSizePx,
                cueTextSizePx,
                bottomPaddingFraction,
                canvas,
                left,
                top,
                right,
                bottom
            )
        }
    }

    private fun resolveCueTextSize(cue: Cue, rawViewHeight: Int, viewHeightMinusPadding: Int): Float {
        if (cue.textSizeType == Cue.TYPE_UNSET || cue.textSize == Cue.DIMEN_UNSET) {
            return 0f
        }
        val defaultCueTextSizePx = resolveTextSize(cue.textSizeType, cue.textSize, rawViewHeight, viewHeightMinusPadding)
        return Math.max(defaultCueTextSizePx, 0f)
    }

    private fun resolveTextSize(
        @Cue.TextSizeType textSizeType: Int,
        textSize: Float,
        rawViewHeight: Int,
        viewHeightMinusPadding: Int
    ): Float {
        return when (textSizeType) {
            Cue.TEXT_SIZE_TYPE_ABSOLUTE -> textSize
            Cue.TEXT_SIZE_TYPE_FRACTIONAL -> textSize * viewHeightMinusPadding
            Cue.TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING -> textSize * rawViewHeight
            Cue.TYPE_UNSET -> Cue.DIMEN_UNSET
            else -> Cue.DIMEN_UNSET
        }
    }

    companion object {

        /**
         * The default fractional text size.
         *
         * @see .setFractionalTextSize
         */
        const val DEFAULT_TEXT_SIZE_FRACTION = 0.0533f

        /**
         * The default bottom padding to apply when [Cue.line] is [Cue.DIMEN_UNSET], as a
         * fraction of the viewport height.
         *
         * @see .setBottomPaddingFraction
         */
        const val DEFAULT_BOTTOM_PADDING_FRACTION = 0.08f
    }

}
/**
 * Sets the text size to be a fraction of the view's remaining height after its top and bottom
 * padding have been subtracted.
 *
 *
 * Equivalent to `#setFractionalTextSize(fractionOfHeight, false)`.
 *
 * @param fractionOfHeight A fraction between 0 and 1.
 */
