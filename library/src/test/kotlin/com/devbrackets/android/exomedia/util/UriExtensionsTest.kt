package com.devbrackets.android.exomedia.util

import android.net.Uri
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [21])
class UriExtensionsTest {

  @Test
  fun getExtensionMpd() {
    val uri = Uri.parse("http://www.example.com/manifest.mpd")

    val actual = uri.getExtension()

    Assert.assertEquals(".mpd", actual)
  }

  @Test
  fun getExtensionMpdPaths() {
    val uri = Uri.parse("http://www.example.com/a/b/c/d/efg/h/manifest.mpd")

    val actual = uri.getExtension()

    Assert.assertEquals(".mpd", actual)
  }

  @Test
  fun getExtensionM3u8() {
    val uri = Uri.parse("http://www.example.com/manifest.m3u8")

    val actual = uri.getExtension()

    Assert.assertEquals(".m3u8", actual)
  }

  @Test
  fun getExtensionM3u8Uppercase() {
    val uri = Uri.parse("http://www.example.com/manifest.M3U8")

    val actual = uri.getExtension()

    Assert.assertEquals(".m3u8", actual)
  }

  @Test
  fun getExtensionPathManifest() {
    val uri = Uri.parse("http://www.example.com/TearsOfSteelTeaser.ism/manifest")

    val actual = uri.getExtension()

    Assert.assertEquals(".ism", actual)
  }

  @Test
  fun getExtensionNone() {
    val uri = Uri.parse("http://www.example.com")

    val actual = uri.getExtension()

    Assert.assertTrue(actual.isEmpty())
  }

  @Test
  fun getExtensionEmpty() {
    val uri = Uri.EMPTY

    val actual = uri.getExtension()

    Assert.assertTrue(actual.isEmpty())
  }

  @Test
  fun getExtensionLastPathSegment() {
    val uri = Uri.parse("http://www.example.com/mp4")

    val actual = uri.getExtension()

    Assert.assertEquals(".mp4", actual)
  }
}