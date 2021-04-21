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


package com.devbrackets.android.exomedia.nmp.manager

import androidx.annotation.Size
import com.google.android.exoplayer2.Player

/**
 * Handles tracking the state of an ExoPlayer to help determine playback
 * readiness, seeking, etc.
 */
internal class StateStore {
  companion object {
    const val FLAG_PLAY_WHEN_READY = -0x10000000
    const val STATE_SEEKING = 100
  }

  //We keep the last few states because that is all we need currently
  private val prevStates = intArrayOf(Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE)

  val mostRecentState: Int
    get() = prevStates[3]

  val isLastReportedPlayWhenReady: Boolean
    get() = prevStates[3] and FLAG_PLAY_WHEN_READY != 0

  fun reset() {
    for (i in prevStates.indices) {
      prevStates[i] = Player.STATE_IDLE
    }
  }

  fun setMostRecentState(playWhenReady: Boolean, state: Int) {
    val newState = getState(playWhenReady, state)
    if (prevStates[3] == newState) {
      return
    }

    prevStates[0] = prevStates[1]
    prevStates[1] = prevStates[2]
    prevStates[2] = prevStates[3]
    prevStates[3] = state
  }

  fun getState(playWhenReady: Boolean, state: Int): Int {
    return state or if (playWhenReady) FLAG_PLAY_WHEN_READY else 0
  }

  fun seekCompleted(): Boolean {
    // Because the playWhenReady isn't a state in itself, rather a flag to a state we will ignore informing of
    // see events when that is the only change.
    if (matchesHistory(intArrayOf(STATE_SEEKING, Player.STATE_BUFFERING, Player.STATE_READY), true)) {
      return true
    }

    // Some devices we get states ordered as [buffering, seeking, ready]
    if (matchesHistory(intArrayOf(Player.STATE_BUFFERING, STATE_SEEKING, Player.STATE_READY), true)) {
      return true
    }

    // Some devices we get states ordered as [seeking, ready, buffering, ready]
    return matchesHistory(intArrayOf(STATE_SEEKING, Player.STATE_READY, Player.STATE_BUFFERING, Player.STATE_READY), true)
  }

  fun matchesHistory(@Size(min = 1, max = 4) states: IntArray, ignorePlayWhenReady: Boolean): Boolean {
    var flag = true
    val andFlag = if (ignorePlayWhenReady) FLAG_PLAY_WHEN_READY.inv() else 0x0.inv()
    val startIndex = prevStates.size - states.size

    for (i in startIndex until prevStates.size) {
      flag = flag and (prevStates[i] and andFlag == states[i - startIndex] and andFlag)
    }

    return flag
  }
}