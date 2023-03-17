package com.devbrackets.android.exomedia.fallback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import java.util.*

/**
 * A simple [Timeline] for the fallback / native media players that only support
 * single media item playback from a URI
 */
@OptIn(UnstableApi::class)
internal class FallbackTimeline(
  private val mediaUri: Uri,
  private val mediaDurationUs: Long
): Timeline() {
  private val window by lazy {
    Window().apply {
      set(
        Window.SINGLE_WINDOW_UID,
        MediaItem.fromUri(mediaUri),
        null,
        C.TIME_UNSET,
        C.TIME_UNSET,
        C.TIME_UNSET,
        true,
        false,
        null,
        0L,
        mediaDurationUs,
        0,
        0,
        0L
      )
    }
  }

  private val periodUid = UUID.randomUUID()
  private val period by lazy {
    Period().apply {
      set(
        periodUid,
        periodUid,
        0,
        durationUs,
        0
      )
    }
  }

  override fun getWindowCount(): Int {
    return 1
  }

  override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
    return window
  }

  override fun getPeriodCount(): Int {
    return 1
  }

  override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
    return period
  }

  override fun getIndexOfPeriod(uid: Any): Int {
    return when (uid) {
      periodUid -> 0
      else -> C.INDEX_UNSET
    }
  }

  override fun getUidOfPeriod(periodIndex: Int): Any {
    return periodUid
  }
}