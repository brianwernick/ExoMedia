package com.devbrackets.android.exomedia.util

import org.junit.Assert
import org.junit.Test

class LongExtensionsTest {

  @Test
  fun millisToFormattedDurationSeconds() {
    val timeMills = 10_000L
    
    val actual = timeMills.millisToFormattedDuration()
    
    Assert.assertEquals("00:10", actual)
  }

  @Test
  fun millisToFormattedDurationMinues() {
    val timeMills = 80_000L

    val actual = timeMills.millisToFormattedDuration()

    Assert.assertEquals("01:20", actual)
  }

  @Test
  fun millisToFormattedDurationHours() {
    val timeMills = 5_000_000L

    val actual = timeMills.millisToFormattedDuration()

    Assert.assertEquals("1:23:20", actual)
  }

  @Test
  fun millisToFormattedDurationNegative() {
    val timeMills = -10_000L

    val actual = timeMills.millisToFormattedDuration()

    Assert.assertEquals("--:--", actual)
  }

  @Test
  fun millisToFormattedDurationZero() {
    val timeMills = 0L

    val actual = timeMills.millisToFormattedDuration()

    Assert.assertEquals("00:00", actual)
  }
}