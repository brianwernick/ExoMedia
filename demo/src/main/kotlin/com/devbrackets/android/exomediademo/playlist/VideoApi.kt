package com.devbrackets.android.exomediademo.playlist

import android.net.Uri
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.playlistcore.data.PlaybackState
import com.devbrackets.android.playlistcore.listener.PlaylistListener
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager

class VideoApi(var videoView: VideoView) : BaseMediaApi(), PlaylistListener<MediaItem> {

    override val isPlaying: Boolean
        get() = videoView.isPlaying

    override val handlesOwnAudioFocus: Boolean
        get() = false

    override val currentPosition: Long
        get() = if (prepared) videoView.currentPosition else 0

    override val duration: Long
        get() = if (prepared) videoView.duration else 0

    override val bufferedPercent: Int
        get() = bufferPercent

    init {
        videoView.setOnErrorListener(this)
        videoView.setOnPreparedListener(this)
        videoView.setOnCompletionListener(this)
        videoView.setOnSeekCompletionListener(this)
        videoView.setOnBufferUpdateListener(this)
    }

    override fun play() {
        videoView.start()
    }

    override fun pause() {
        videoView.pause()
    }

    override fun stop() {
        videoView.stopPlayback()
    }

    override fun reset() {
        // Purposefully left blank
    }

    override fun release() {
        videoView.suspend()
    }

    override fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float) {
        videoView.volume = (left + right) / 2
    }

    override fun seekTo(@IntRange(from = 0L) milliseconds: Long) {
        videoView.seekTo(milliseconds.toInt().toLong())
    }

    override fun handlesItem(item: MediaItem): Boolean {
        return item.mediaType == BasePlaylistManager.VIDEO
    }

    override fun playItem(item: MediaItem) {
        prepared = false
        bufferPercent = 0
        videoView.setVideoURI(Uri.parse(if (item.downloaded) item.downloadedMediaUri else item.mediaUrl))
    }

    /*
     * PlaylistListener methods used for keeping the VideoControls provided
     * by the ExoMedia VideoView up-to-date with the current playback state
     */
    override fun onPlaylistItemChanged(currentItem: MediaItem?, hasNext: Boolean, hasPrevious: Boolean): Boolean {
        videoView.videoControls?.let { controls ->
            // Updates the VideoControls display text
            controls.setTitle(currentItem?.title ?: "")
            controls.setSubTitle(currentItem?.album ?: "")
            controls.setDescription(currentItem?.artist ?: "")

            // Updates the VideoControls button visibilities
            controls.setPreviousButtonEnabled(hasPrevious)
            controls.setNextButtonEnabled(hasNext)
        }

        return false
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackState): Boolean {
        return false
    }
}
