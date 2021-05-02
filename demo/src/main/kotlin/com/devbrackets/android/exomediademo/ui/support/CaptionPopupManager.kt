package com.devbrackets.android.exomediademo.ui.support

import android.util.Log
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomediademo.R

class CaptionPopupManager {
  companion object {
    const val CC_GROUP_INDEX_MOD = 1000
    const val CC_DISABLED = -1001
    const val CC_DEFAULT = -1000
  }

  data class CaptionItem(
      val id: Int,
      val title: String,
      val selected: Boolean
  )

  fun getCaptionItems(videoView: VideoView): List<CaptionItem> {
    val trackGroupArray = videoView.availableTracks?.get(RendererType.CLOSED_CAPTION)
    if (trackGroupArray == null || trackGroupArray.isEmpty) {
      return emptyList()
    }

    val items = mutableListOf<CaptionItem>()
    var trackSelected = false

    for (groupIndex in 0 until trackGroupArray.length) {
      val selectedIndex = videoView.getSelectedTrackIndex(RendererType.CLOSED_CAPTION, groupIndex)
      Log.d("Captions", "Selected Caption Track: $groupIndex | $selectedIndex")
      val trackGroup = trackGroupArray.get(groupIndex)
      for (index in 0 until trackGroup.length) {
        val format = trackGroup.getFormat(index)

        // Skip over non text formats.
        if (!format.sampleMimeType!!.startsWith("text")) {
          continue
        }

        val title = format.label ?: format.language ?: "${groupIndex.toShort()}:$index"
        val itemId = groupIndex * CC_GROUP_INDEX_MOD + index
        items.add(CaptionItem(itemId, title, index == selectedIndex))
        if (index == selectedIndex) {
          trackSelected = true
        }
      }
    }

    // Adds "Disabled" and "Auto" options
    val rendererEnabled = videoView.isRendererEnabled(RendererType.CLOSED_CAPTION)
    items.add(0, CaptionItem(CC_DEFAULT, videoView.context.getString(R.string.auto), rendererEnabled && !trackSelected))
    items.add(0, CaptionItem(CC_DISABLED, videoView.context.getString(R.string.disable), !rendererEnabled))

    return items
  }

  fun showCaptionsMenu(items: List<CaptionItem>, button: View, clickListener: PopupMenu.OnMenuItemClickListener) {
    val context = button.context
    val popupMenu = PopupMenu(context, button)
    val menu = popupMenu.menu

    // Add Menu Items
    items.forEach {
      val item = menu.add(0, it.id, 0, it.title)

      item.isCheckable = true
      if (it.selected) {
        item.isChecked = true
      }
    }

    menu.setGroupCheckable(0, true, true)
    popupMenu.setOnMenuItemClickListener(clickListener)
    popupMenu.show()
  }
}