package com.devbrackets.android.exomediademo.playlist.manager

import android.app.Application
import com.devbrackets.android.exomedia.ui.listener.VideoControlsButtonListener
import com.devbrackets.android.exomedia.ui.widget.controls.DefaultVideoControls
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.exomediademo.playlist.VideoApi
import com.devbrackets.android.exomediademo.playlist.service.MediaService
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager

/**
 * A PlaylistManager that extends the [ListPlaylistManager] for use with the
 * [MediaService] which extends [com.devbrackets.android.playlistcore.service.BasePlaylistService].
 */
class PlaylistManager(application: Application) : ListPlaylistManager<MediaItem>(application, MediaService::class.java) {

    /**
     * Note: You can call [.getMediaPlayers] and add it manually in the activity,
     * however we have this helper method to allow registration of the media controls
     * repeatListener provided by ExoMedia's [com.devbrackets.android.exomedia.ui.widget.DefaultVideoControls]
     */
    fun addVideoApi(videoApi: VideoApi) {
        mediaPlayers.add(videoApi)
        updateVideoControls(videoApi)
        registerPlaylistListener(videoApi)
    }

    /**
     * Note: You can call [.getMediaPlayers] and remove it manually in the activity,
     * however we have this helper method to remove the registered repeatListener from [.addVideoApi]
     */
    fun removeVideoApi(videoApi: VideoApi) {
        (videoApi.videoView.videoControls as? DefaultVideoControls)?.setButtonListener(null)

        unRegisterPlaylistListener(videoApi)
        mediaPlayers.remove(videoApi)
    }

    /**
     * Updates the available controls on the VideoView and links the
     * button events to the playlist service and this.
     *
     * @param videoApi The VideoApi to link
     */
    private fun updateVideoControls(videoApi: VideoApi) {
        (videoApi.videoView.videoControls as? DefaultVideoControls)?.let {
            it.setPreviousButtonRemoved(false)
            it.setNextButtonRemoved(false)
            it.setButtonListener(ControlsListener())
        }
    }

    /**
     * An implementation of the [VideoControlsButtonListener] that provides
     * integration with the playlist service.
     */
    private inner class ControlsListener : VideoControlsButtonListener {
        override fun onPlayPauseClicked(): Boolean {
            invokePausePlay()
            return true
        }

        override fun onPreviousClicked(): Boolean {
            invokePrevious()
            return false
        }

        override fun onNextClicked(): Boolean {
            invokeNext()
            return false
        }

        override fun onRewindClicked(): Boolean {
            return false
        }

        override fun onFastForwardClicked(): Boolean {
            return false
        }
    }
}