package com.devbrackets.android.exomediademo.data

import com.devbrackets.android.playlistcore.annotation.SupportedMediaType
import com.devbrackets.android.playlistcore.api.PlaylistItem
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager

/**
 * A custom [PlaylistItem]
 * to hold the information pertaining to the audio and video items
 */
class MediaItem(private val sample: Samples.Sample, internal var isAudio: Boolean) : PlaylistItem {

    override val id: Long
        get() = 0

    override val downloaded: Boolean
        get() = false

    @SupportedMediaType
    override val mediaType: Int
        get() = if (isAudio) BasePlaylistManager.AUDIO else BasePlaylistManager.VIDEO

    override val mediaUrl: String?
        get() = sample.mediaUrl

    override val downloadedMediaUri: String?
        get() = null

    override val thumbnailUrl: String?
        get() = sample.artworkUrl

    override val artworkUrl: String?
        get() = sample.artworkUrl

    override val title: String?
        get() = sample.title

    override val album: String?
        get() = "PlaylistCore Demo"

    override val artist: String?
        get() = "Unknown Artist"
}