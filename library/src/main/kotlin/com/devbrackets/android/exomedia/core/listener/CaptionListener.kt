package com.devbrackets.android.exomedia.core.listener


import androidx.media3.common.text.Cue

/**
 * A listener for receiving notifications of timed text.
 */
interface CaptionListener {
  fun onCues(cues: List<Cue>)
}
