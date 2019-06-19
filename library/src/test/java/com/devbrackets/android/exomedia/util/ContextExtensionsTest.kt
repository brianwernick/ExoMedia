package com.devbrackets.android.exomedia.util

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ContextExtensionsTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun `isDeviceTV returns true when ui configuration is UI_MODE_TYPE_TELEVISION`() {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        shadowOf(uiModeManager).currentModeType = Configuration.UI_MODE_TYPE_TELEVISION
        assertThat(context.isDeviceTV()).isTrue()
    }

    @Test
    fun `isDeviceTV returns false when ui configuration is other than UI_MODE_TYPE_TELEVISION`() {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        Configuration.UI_MODE_TYPE_MASK
                .downTo(Configuration.UI_MODE_TYPE_UNDEFINED)
                .filter { it != Configuration.UI_MODE_TYPE_TELEVISION }
                .forEach {
                    shadowOf(uiModeManager).currentModeType = it
                    assertThat(context.isDeviceTV()).isFalse()
                }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.KITKAT])
    fun `isDeviceTV returns false when when ui configuration is UI_MODE_TYPE_TELEVISION but device sdk is lower than lollipop`() {
        assertThat(context.isDeviceTV()).isFalse()
    }
}