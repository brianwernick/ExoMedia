package com.devbrackets.android.exomedia.nmp.manager

import android.annotation.SuppressLint
import android.os.Build
import com.devbrackets.android.exomedia.BuildConfig

/**
 * Provides the user agent to use when communicating over a network
 */
open class UserAgentProvider {
  companion object {
    @SuppressLint("DefaultLocale")
    val defaultUserAgent = String.format(
        "ExoMedia %s (%d) / Android %s / %s",
        BuildConfig.EXO_MEDIA_VERSION_NAME,
        BuildConfig.EXO_MEDIA_VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.MODEL
    )
  }

  open val userAgent: String = defaultUserAgent
}