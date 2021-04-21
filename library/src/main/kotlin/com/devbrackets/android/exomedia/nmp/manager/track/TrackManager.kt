/*
 * Copyright (C) 2017 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.nmp.manager.track

import android.content.Context
import android.util.ArrayMap
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import java.util.ArrayList

/**
 * Handles managing the tracks for the [CorePlayer]
 */
class TrackManager(context: Context) {
  private val selectionFactory: AdaptiveTrackSelection.Factory = AdaptiveTrackSelection.Factory()
  val selector: DefaultTrackSelector = DefaultTrackSelector(context, selectionFactory)


  /**
   * Retrieves a list of available tracks
   *
   * @return A list of available tracks associated with each type
   */
  @Suppress("FoldInitializerAndIfToElvis")
  fun getAvailableTracks(): Map<RendererType, TrackGroupArray>? {
    val mappedTrackInfo = selector.currentMappedTrackInfo
    if (mappedTrackInfo == null) {
      return null
    }

    val trackMap = ArrayMap<RendererType, TrackGroupArray>()

    RendererType.values().forEach { type ->
      val trackGroups = ArrayList<TrackGroup>()
      for (exoPlayerTrackIndex in getExoPlayerTracksInfo(type, 0, mappedTrackInfo).indexes) {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex)
        for (i in 0 until trackGroupArray.length) {
          trackGroups.add(trackGroupArray.get(i))
        }
      }

      if (trackGroups.isNotEmpty()) {
        trackMap[type] = TrackGroupArray(*trackGroups.toTypedArray())
      }
    }

    return trackMap
  }

  fun getExoPlayerTracksInfo(type: RendererType, groupIndex: Int, mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?): RendererTrackInfo {
    if (mappedTrackInfo == null) {
      return RendererTrackInfo(emptyList(), C.INDEX_UNSET, C.INDEX_UNSET)
    }

    // holder for the all exo player renderer track indexes of the specified renderer type
    val rendererTrackIndexes = ArrayList<Int>()
    var rendererTrackIndex = C.INDEX_UNSET

    // the corrected exoplayer group index
    var rendererTrackGroupIndex = C.INDEX_UNSET
    var skippedRenderersGroupsCount = 0


    for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
      if (type.exoPlayerTrackType != mappedTrackInfo.getRendererType(rendererIndex)) {
        continue
      }

      rendererTrackIndexes.add(rendererIndex)
      val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
      if (skippedRenderersGroupsCount + trackGroups.length <= groupIndex) {
        skippedRenderersGroupsCount += trackGroups.length
        continue
      }

      // if the groupIndex belongs to the current exo player renderer
      if (rendererTrackIndex == C.INDEX_UNSET) {
        rendererTrackIndex = rendererIndex
        rendererTrackGroupIndex = groupIndex - skippedRenderersGroupsCount
      }
    }

    return RendererTrackInfo(rendererTrackIndexes, rendererTrackIndex, rendererTrackGroupIndex)
  }

  @JvmOverloads
  fun getSelectedTrackIndex(type: RendererType, groupIndex: Int = 0): Int {
    // Retrieves the available tracks
    val mappedTrackInfo = selector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo)
    if (tracksInfo.index == C.INDEX_UNSET) {
      return -1
    }

    val trackGroupArray = mappedTrackInfo!!.getTrackGroups(tracksInfo.index)
    if (trackGroupArray.length == 0) {
      return -1
    }

    // Verifies the track selection has been overridden
    val selectionOverride = selector.parameters.getSelectionOverride(tracksInfo.index, trackGroupArray)
    if (selectionOverride == null || selectionOverride.groupIndex != tracksInfo.groupIndex || selectionOverride.length <= 0) {
      return -1
    }

    // In the current implementation only one track can be selected at a time so get the first one.
    return selectionOverride.tracks[0]
  }

  fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    // Retrieves the available tracks
    val mappedTrackInfo = selector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo)
    if (tracksInfo.index == C.INDEX_UNSET || mappedTrackInfo == null) {
      return
    }

    val trackGroupArray = mappedTrackInfo.getTrackGroups(tracksInfo.index)
    if (trackGroupArray.length == 0 || trackGroupArray.length <= tracksInfo.groupIndex) {
      return
    }

    // Finds the requested group
    val group = trackGroupArray.get(tracksInfo.groupIndex)
    if (group.length <= trackIndex) {
      return
    }

    val parametersBuilder = selector.buildUponParameters()
    for (rendererTrackIndex in tracksInfo.indexes) {
      parametersBuilder.clearSelectionOverrides(rendererTrackIndex)

      // Disable renderers of the same type to prevent playback errors
      if (tracksInfo.index != rendererTrackIndex) {
        parametersBuilder.setRendererDisabled(rendererTrackIndex, true)
        continue
      }

      // Specifies the correct track to use
      parametersBuilder.setSelectionOverride(rendererTrackIndex, trackGroupArray,
          DefaultTrackSelector.SelectionOverride(tracksInfo.groupIndex, trackIndex))

      // make sure renderer is enabled
      parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
    }

    selector.setParameters(parametersBuilder)
  }

  /**
   * Clear all selected tracks for the specified renderer and re-enable all renderers so the player can select the default track.
   *
   * @param type The renderer type
   */
  fun clearSelectedTracks(type: RendererType) {
    // Retrieves the available tracks
    val mappedTrackInfo = selector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    val parametersBuilder = selector.buildUponParameters()

    for (rendererTrackIndex in tracksInfo.indexes) {
      // Reset all renderers re-enabling so the player can select the streams default track.
      parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
          .clearSelectionOverrides(rendererTrackIndex)
    }

    selector.setParameters(parametersBuilder)
  }

  fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    val mappedTrackInfo = selector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    if (tracksInfo.indexes.isEmpty()) {
      return
    }

    var enabledSomething = false
    val parametersBuilder = selector.buildUponParameters()

    for (rendererTrackIndex in tracksInfo.indexes) {
      if (!enabled) {
        parametersBuilder.setRendererDisabled(rendererTrackIndex, true)
        continue
      }

      val selectionOverride = selector.parameters.getSelectionOverride(rendererTrackIndex, mappedTrackInfo!!.getTrackGroups(rendererTrackIndex))
      // check whether the renderer has been selected before
      // other renderers should be kept disabled to avoid playback errors
      if (selectionOverride != null) {
        parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
        enabledSomething = true
      }
    }

    if (enabled && !enabledSomething) {
      // if nothing has been enabled enable the first sequential renderer
      parametersBuilder.setRendererDisabled(tracksInfo.indexes[0], false)
    }

    selector.setParameters(parametersBuilder)
  }

  /**
   * Return true if at least one renderer for the given type is enabled
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: RendererType): Boolean {
    val mappedTrackInfo = selector.currentMappedTrackInfo
    val tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo)
    val parameters = selector.parameters

    return tracksInfo.indexes.any {
      !parameters.getRendererDisabled(it)
    }
  }
}