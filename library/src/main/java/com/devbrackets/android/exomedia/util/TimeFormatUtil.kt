/*
 * Copyright (C) 2015 -  2018 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.util

import android.text.format.DateUtils
import java.util.*

object TimeFormatUtil {
    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())

    /**
     * Formats the specified milliseconds to a human readable format
     * in the form of (Hours : Minutes : Seconds).  If the specified
     * milliseconds is less than 0 the resulting format will be
     * "--:--" to represent an unknown time
     *
     * @param milliseconds The time in milliseconds to format
     * @return The human readable time
     */
    fun formatMs(milliseconds: Long): String {
        if (milliseconds < 0) {
            return "--:--"
        }

        val seconds = milliseconds % DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS
        val minutes = milliseconds % DateUtils.HOUR_IN_MILLIS / DateUtils.MINUTE_IN_MILLIS
        val hours = milliseconds % DateUtils.DAY_IN_MILLIS / DateUtils.HOUR_IN_MILLIS

        formatBuilder.setLength(0)
        return if (hours > 0) {
            formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else formatter.format("%02d:%02d", minutes, seconds).toString()

    }
}
