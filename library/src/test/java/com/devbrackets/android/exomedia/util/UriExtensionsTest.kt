package com.devbrackets.android.exomedia.util

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class UriExtensionsTest {

    @Test
    fun `getExtension extracts extension from url`() {
        assertThat(Uri.parse("http://www.example.com/manifest.mpd").getExtension()).isEqualTo(".mpd")
    }

    @Test
    fun `getExtension extracts extension from url with many path segments`() {
        assertThat(Uri.parse("http://www.example.com/a/b/c/d/efg/h/manifest.mpd").getExtension()).isEqualTo(".mpd")
    }

    @Test
    fun `getExtension extracts extension from url when extension is m3u8`() {
        assertThat(Uri.parse("http://www.example.com/manifest.m3u8").getExtension()).isEqualTo(".m3u8")
    }

    @Test
    fun `getExtension extracts lowercased extension from url when extension is uppercase`() {
        assertThat(Uri.parse("http://www.example.com/manifest.M3U8").getExtension()).isEqualTo(".m3u8")
    }

    @Test
    fun `getExtension extracts extension from url when extension is specified before last path segment`() {
        assertThat(Uri.parse("http://www.example.com/TearsOfSteelTeaser.ism/manifest").getExtension()).isEqualTo(".ism")
    }

    @Test
    fun `getExtension returns empty string when extension is not specified`() {
        assertThat(Uri.parse("http://www.example.com").getExtension()).isEmpty()
    }

    @Test
    fun `getExtension returns empty string when uri is empty`() {
        assertThat(Uri.EMPTY.getExtension()).isEmpty()
    }

    @Test
    fun `getExtension converts last path segment to extension if not a real extension is specified`() {
        assertThat(Uri.parse("http://www.example.com/mp4").getExtension()).isEqualTo(".mp4")
    }
}