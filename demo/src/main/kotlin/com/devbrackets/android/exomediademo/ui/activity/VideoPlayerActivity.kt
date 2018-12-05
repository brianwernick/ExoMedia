package com.devbrackets.android.exomediademo.ui.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.MenuItem
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.listener.VideoControlsSeekListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls
import com.devbrackets.android.exomediademo.App
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.manager.PlaylistManager
import com.devbrackets.android.exomediademo.playlist.VideoApi
import com.google.android.exoplayer2.util.EventLogger
import kotlinx.android.synthetic.main.video_player_activity.*

open class VideoPlayerActivity : Activity(), VideoControlsSeekListener {
    companion object {
        const val EXTRA_INDEX = "EXTRA_INDEX"
        const val PLAYLIST_ID = 6 //Arbitrary, for the example (different from audio)

        private const val CC_GROUP_INDEX_MOD = 1000
        private const val CC_DISABLED = -1001
        private const val CC_DEFAULT = -1000
    }

    private lateinit var videoApi: VideoApi
    private lateinit var playlistManager: PlaylistManager
    private lateinit var captionsButton: AppCompatImageButton

    private val selectedIndex by lazy { intent.extras?.getInt(EXTRA_INDEX, 0) ?: 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player_activity)

        init()
    }

    override fun onStop() {
        super.onStop()
        playlistManager.removeVideoApi(videoApi)
        playlistManager.invokeStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        playlistManager.invokeStop()
    }

    override fun onSeekStarted(): Boolean {
        playlistManager.invokeSeekStarted()
        return true
    }

    override fun onSeekEnded(seekTime: Long): Boolean {
        playlistManager.invokeSeekEnded(seekTime)
        return true
    }

    private fun init() {
        setupPlaylistManager()

        videoView.setHandleAudioFocus(false)
        videoView.setAnalyticsListener(EventLogger(null))

        setupClosedCaptions()

        videoApi = VideoApi(videoView)
        playlistManager.addVideoApi(videoApi)
        playlistManager.play(0, false)
    }

    private fun setupClosedCaptions() {
        captionsButton = AppCompatImageButton(this).apply {
            setBackgroundResource(android.R.color.transparent)
            setImageResource(R.drawable.ic_closed_caption_white_24dp)
            setOnClickListener { showCaptionsMenu() }
        }

        (videoView.videoControlsCore as? VideoControls)?.let {
            it.setSeekListener(this)
            if (videoView.trackSelectionAvailable()) {
                it.addExtraView(captionsButton)
            }
        }

        videoView.setOnVideoSizedChangedListener { intrinsicWidth, intrinsicHeight, pixelWidthHeightRatio ->
            val videoAspectRatio: Float = if (intrinsicWidth == 0 || intrinsicHeight == 0) {
                1f
            } else {
                intrinsicWidth * pixelWidthHeightRatio / intrinsicHeight
            }

            subtitleFrameLayout.setAspectRatio(videoAspectRatio)
        }

        videoView.setCaptionListener(subtitleView)
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private fun setupPlaylistManager() {
        playlistManager = (applicationContext as App).playlistManager

        val mediaItems = Samples.getVideoSamples().map {
            MediaItem(it, false)
        }

        playlistManager.setParameters(mediaItems, selectedIndex)
        playlistManager.id = PLAYLIST_ID.toLong()
    }

    private fun showCaptionsMenu() {
        val availableTracks = videoView.availableTracks ?: return
        val trackGroupArray = availableTracks[ExoMedia.RendererType.CLOSED_CAPTION]
        if (trackGroupArray == null || trackGroupArray.isEmpty) {
            return
        }

        val popupMenu = PopupMenu(this, captionsButton)
        val menu = popupMenu.menu

        // Add Menu Items
        val disabledItem = menu.add(0, CC_DISABLED, 0, getString(R.string.disable)).apply {
            isCheckable = true
        }

        val defaultItem = menu.add(0, CC_DEFAULT, 0, getString(R.string.auto)).apply {
            isCheckable = true
        }

        var selected = false
        for (groupIndex in 0 until trackGroupArray.length) {
            val selectedIndex = videoView.getSelectedTrackIndex(ExoMedia.RendererType.CLOSED_CAPTION, groupIndex)
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
                val item = menu.add(0, itemId, 0, title)

                item.isCheckable = true
                if (index == selectedIndex) {
                    item.isChecked = true
                    selected = true
                }
            }
        }

        if (!selected) {
            if (videoView.isRendererEnabled(ExoMedia.RendererType.CLOSED_CAPTION)) {
                defaultItem.isChecked = true
            } else {
                disabledItem.isChecked = true
            }
        }

        menu.setGroupCheckable(0, true, true)
        popupMenu.setOnMenuItemClickListener { menuItem -> onTrackSelected(menuItem) }
        popupMenu.show()
    }

    private fun onTrackSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        val itemId = menuItem.itemId

        when (itemId) {
            CC_DEFAULT -> videoView.clearSelectedTracks(ExoMedia.RendererType.CLOSED_CAPTION)
            CC_DISABLED -> videoView.setRendererEnabled(ExoMedia.RendererType.CLOSED_CAPTION, false)
            else -> {
                val trackIndex = itemId % CC_GROUP_INDEX_MOD
                val groupIndex = itemId / CC_GROUP_INDEX_MOD
                videoView.setTrack(ExoMedia.RendererType.CLOSED_CAPTION, groupIndex, trackIndex)
            }
        }

        return true
    }
}
