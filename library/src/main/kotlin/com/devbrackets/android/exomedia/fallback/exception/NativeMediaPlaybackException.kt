package com.devbrackets.android.exomedia.fallback.exception

import android.media.MediaPlayer

/**
 * An Exception to indicate that the Native [MediaPlayer] has
 * had an error during or while attempting playback
 */
class NativeMediaPlaybackException(
    val what: Int,
    val extra: Int
) : Exception(MediaPlayer::class.java.name + " has had the error " + what + " with extras " + extra)
