package com.devbrackets.android.exomedia.nmp.manager.track

data class RendererTrackInfo(
  /**
   * The exo player renderer track indexes
   */
  val indexes: List<Int>,

  /**
   * The renderer track index related to the requested `groupIndex`
   */
  val index: Int,

  /**
   * The corrected exoplayer group index which may be used to obtain proper track group from the renderer
   */
  val groupIndex: Int
)