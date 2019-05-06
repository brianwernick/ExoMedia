package com.devbrackets.android.exomedia.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LongExtensionsTest {

    @Test
    fun `millisToFormattedTimeString formats millis in expected form if value is lesser than minute`() {
        assertThat(10000L.millisToFormattedTimeString()).isEqualTo("00:10")
    }

    @Test
    fun `millisToFormattedTimeString formats millis in expected form if value is lesser than hour`() {
        assertThat(80000L.millisToFormattedTimeString()).isEqualTo("01:20")
    }

    @Test
    fun `millisToFormattedTimeString formats millis in expected form if value is bigger than hour`() {
        assertThat(5000000L.millisToFormattedTimeString()).isEqualTo("1:23:20")
    }

    @Test
    fun `millisToFormattedTimeString omits count of days if millis value is bigger than day`() {
        assertThat(100000000L.millisToFormattedTimeString()).isEqualTo("3:46:40")
    }

    @Test
    fun `millisToFormattedTimeString formats millis in expected form if value is lesser than zero`() {
        assertThat((-1L).millisToFormattedTimeString()).isEqualTo("--:--")
    }

    @Test
    fun `millisToFormattedTimeString formats millis in expected form if value is zero`() {
        assertThat(0L.millisToFormattedTimeString()).isEqualTo("00:00")
    }
}