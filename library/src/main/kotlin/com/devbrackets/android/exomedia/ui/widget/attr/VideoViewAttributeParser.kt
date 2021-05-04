/*
 * Copyright (C) 2016 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.ui.widget.attr

import android.content.Context
import android.util.AttributeSet
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.nmp.config.DefaultPlayerConfigProvider
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigProvider

class VideoViewAttributeParser {

  fun parse(context: Context, attrs: AttributeSet?): VideoViewAttributes {
    if (attrs == null) {
      return VideoViewAttributes()
    }

    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoView)

    val useDefaultControls = typedArray.getBoolean(R.styleable.VideoView_useDefaultControls, false)
    val useTextureViewBacking = typedArray.getBoolean(R.styleable.VideoView_useTextureViewBacking, false)

    val scaleType = if (typedArray.hasValue(R.styleable.VideoView_videoScale)) {
      ScaleType.fromOrdinal(typedArray.getInt(R.styleable.VideoView_videoScale, -1))
    } else {
      null
    }

    val measureBasedOnAspectRatio = if (typedArray.hasValue(R.styleable.VideoView_measureBasedOnAspectRatio)) {
      typedArray.getBoolean(R.styleable.VideoView_measureBasedOnAspectRatio, false)
    } else {
      null
    }

    val configProvider = typedArray.getString(R.styleable.VideoView_playerConfigProvider)?.let {
      Class.forName(it).getConstructor().newInstance() as PlayerConfigProvider
    } ?: DefaultPlayerConfigProvider()

    typedArray.recycle()

    return VideoViewAttributes(
        useDefaultControls = useDefaultControls,
        useTextureViewBacking = useTextureViewBacking,
        scaleType = scaleType,
        measureBasedOnAspectRatio = measureBasedOnAspectRatio,
        playerConfigProvider = configProvider
    )
  }
}