/*
 * Copyright (C) 2015-2021 ExoMedia Contributors,
 * Copyright (C) 2015 The Android Open Source Project
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

package com.devbrackets.android.exomedia.core.renderer.audio

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters

class DefaultAudioRenderListener(
    private val delegate: AnalyticsCollector
): AudioListener, AudioRendererEventListener by delegate {
  private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

  override fun onAudioDisabled(counters: DecoderCounters) {
    currentSessionId = C.AUDIO_SESSION_ID_UNSET
    delegate.onAudioDisabled(counters)
  }

  override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
    delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)
  }

  override fun onAudioSessionIdChanged(audioSessionId: Int) {
    currentSessionId = audioSessionId
    delegate.onAudioSessionIdChanged(audioSessionId)
  }

  override fun onAudioUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
    delegate.onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
  }
}