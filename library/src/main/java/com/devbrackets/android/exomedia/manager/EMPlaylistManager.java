/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia.manager;

import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.EMRemoteActions;
import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMPlaylistServiceCallback;
import com.devbrackets.android.exomedia.service.EMPlaylistService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A manager to keep track of a playlist of items that a service can use for playback.
 * Additionally, this manager provides methods for interacting with the specified service
 * to simplify and standardize implementations in the service itself.  This manager can be
 * used as standalone with a custom service, or in conjunction with
 * {@link com.devbrackets.android.exomedia.service.EMPlaylistService}
 */
@SuppressWarnings("unused")
public abstract class EMPlaylistManager<I extends EMPlaylistManager.PlaylistItem> implements EMPlaylistServiceCallback {
    private static final String TAG = "EMPlaylistManager";

    public static final int INVALID_PLAYLIST_ID = -1;
    public static final int INVALID_PLAYLIST_INDEX = -1;

    public enum MediaType {
        AUDIO,
        VIDEO,
        AUDIO_AND_VIDEO,
        OTHER,
        NONE
    }

    public interface PlaylistItem {
        long getId();

        long getPlaylistId();

        MediaType getMediaType();

        String getMediaUrl();

        String getDownloadedMediaUri();

        String getThumbnailUrl();

        String getArtworkUrl();

        String getTitle();

        String getAlbum();

        String getArtist();
    }

    protected List<I> playList;
    protected int currentPosition = 0;
    protected long playListId = INVALID_PLAYLIST_ID;

    protected MediaType allowedType = MediaType.AUDIO;
    protected WeakReference<EMVideoView> videoPlayer = new WeakReference<>(null);

    @Nullable
    protected EMPlaylistService service;
    protected List<EMPlaylistServiceCallback> callbackList = new ArrayList<>();

    @Nullable
    protected PendingIntent playPausePendingIntent, nextPendingIntent, previousPendingIntent, stopPendingIntent, repeatPendingIntent, shufflePendingIntent, seekStartedPendingIntent;
    @Nullable
    protected Intent seekEndedIntent, allowedTypeChangedIntent;

    /**
     * Retrieves the application to use when starting and communicating with the
     * PlaylistService specified with {@link #getMediaServiceClass()}.
     *
     * @return The Application to use when starting and controlling the PlaylistService
     */
    protected abstract Application getApplication();

    /**
     * Retrieves the class that represents the PlaylistService.  This is used when communicating
     * with the service to perform the playback controls.
     *
     * @return The class for the Service to control.  This should extend {@link EMPlaylistService}
     */
    protected abstract Class<? extends Service> getMediaServiceClass();

    /**
     * A basic constructor that will retrieve the application
     * via {@link #getApplication()}.
     */
    public EMPlaylistManager() {
        constructControlIntents(getMediaServiceClass(), getApplication());
    }

    /**
     * A constructor that will use the specified application to initialize
     * the EMPlaylistManager.  This should only be used in instances that
     * {@link #getApplication()} will not be prepared at this point.  This
     * can happen when using Dependence Injection frameworks such as Dagger.
     *
     * @param application The application to use to initialize the EMPlaylistManager
     */
    public EMPlaylistManager(Application application) {
        constructControlIntents(getMediaServiceClass(), application);
    }

    /**
     * This is a pass through method that is called from the {@link EMPlaylistService} to inform
     * any listeners that are registered through {@link #registerServiceCallbacks(EMPlaylistServiceCallback)}
     *
     * @param currentItem The new playback item
     * @param hasNext True if there exists an item after the <code>currentItem</code> in the playlist
     * @param hasPrevious True if there exists an item before the <code>currentItem</code> in the playlist
     * @return True if the event should be consumed
     */
    @Override
    public boolean onPlaylistItemChanged(PlaylistItem currentItem, boolean hasNext, boolean hasPrevious) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onPlaylistItemChanged(currentItem, hasNext, hasPrevious)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This is a pass through method that is called from the {@link EMPlaylistService} to inform
     * any listeners that are registered through {@link #registerServiceCallbacks(EMPlaylistServiceCallback)}
     *
     * @param mediaState The new media playback state
     * @return True if the event should be consumed
     */
    @Override
    public boolean onMediaStateChanged(EMPlaylistService.MediaState mediaState) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onMediaStateChanged(mediaState)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This is a pass through method that is called from the {@link EMPlaylistService} to inform
     * any listeners that are registered through {@link #registerServiceCallbacks(EMPlaylistServiceCallback)}
     *
     * @param event The current media progress event
     * @return True if the event should be consumed
     */
    @Override
    public boolean onProgressUpdated(EMMediaProgressEvent event) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onProgressUpdated(event)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the most recent media playback state.
     *
     * @return The most recent MediaState
     */
    public EMPlaylistService.MediaState getCurrentMediaState() {
        if (service != null) {
            return service.getCurrentMediaState();
        }

        return EMPlaylistService.MediaState.STOPPED;
    }

    /**
     * Retrieves the current progress for the media playback
     *
     * @return The most recent progress event
     */
    @Nullable
    public EMMediaProgressEvent getCurrentProgress() {
        return service != null ? service.getCurrentMediaProgress() : null;
    }

    /**
     * Retrieves the most recent {@link EMPlaylistItemChangedEvent}
     *
     * @return The most recent Item Changed information
     */
    @Nullable
    public EMPlaylistItemChangedEvent getCurrentItemChangedEvent() {
        return service != null ? service.getCurrentItemChangedEvent() : null;
    }

    /**
     * Links the {@link EMPlaylistService} so that we can correctly manage the
     * {@link EMPlaylistServiceCallback}
     *
     * @param service The AudioService to link to this manager
     */
    public void registerService(EMPlaylistService service) {
        this.service = service;
        service.registerCallback(this);
    }

    /**
     * UnLinks the {@link EMPlaylistService} from this manager. (see {@link #registerService(EMPlaylistService)}
     */
    public void unRegisterService() {
        if (service != null) {
            service.unRegisterCallback(this);
            service = null;
        }
    }

    /**
     * Registers the callback to this service.  These callbacks will only be
     * called if {@link #registerService(EMPlaylistService)} has been called.
     *
     * @param callback The callback to register
     */
    public void registerServiceCallbacks(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.add(callback);
        }
    }

    /**
     * UnRegisters the specified callback.  This should be called when the callback
     * class losses focus, or should be destroyed.
     *
     * @param callback The callback to remove
     */
    public void unRegisterServiceCallbacks(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.remove(callback);
        }
    }

    /**
     * A utility method to allow for single line implementations to start playing the media
     * item as specified by the passed parameters.
     *
     * @param playListItems The list of items to play
     * @param startIndex The index in the playlistItems to start playback
     * @param seekPosition The position in the startIndex item to start at (in milliseconds)
     * @param startPaused True if the media item should start paused instead of playing
     */
    public void play(List<I> playListItems, int startIndex, int seekPosition, boolean startPaused) {
        setParameters(playListItems, startIndex);
        play(seekPosition, startPaused);
    }

    /**
     * In order to use this method you must call {@link #setParameters(java.util.List, int)} first.
     * Alternatively you can call {@link #play(java.util.List, int, int, boolean)}
     */
    public void play(int seekPosition, boolean startPaused) {
        I currentItem = getCurrentItem();

        if (currentItem == null) {
            return;
        }

        //Starts the playlist service
        Intent intent = new Intent(getApplication(), getMediaServiceClass());
        intent.setAction(EMRemoteActions.ACTION_START_SERVICE);
        intent.putExtra(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, seekPosition);
        intent.putExtra(EMRemoteActions.ACTION_EXTRA_START_PAUSED, startPaused);
        getApplication().startService(intent);
    }

    /**
     * Sets the List of items to be used for the play list.  This can include both audio
     * and video items.
     *
     * @param playListItems The List of items to play
     * @param startIndex The index in the list to start playback with
     */
    public void setParameters(List<I> playListItems, int startIndex) {
        playList = playListItems;

        setCurrentIndex(startIndex);
        setPlaylistId(INVALID_PLAYLIST_ID);
    }

    /**
     * Sets the ID associated with the current playlist.
     *
     * @param playListId The id for the playlist
     */
    public void setPlaylistId(long playListId) {
        this.playListId = playListId;
    }

    /**
     * Sets the type of media that we can currently play.  When set,
     * the {@link #next()} and {@link #previous()} will skip any items
     * that do not match the allowed type.
     *
     * @param allowedType The media types to allow playback with [default: {@link MediaType#AUDIO_AND_VIDEO}]
     */
    public void setAllowedMediaType(MediaType allowedType) {
        this.allowedType = allowedType;

        //Tries to start the intent
        if (allowedTypeChangedIntent != null) {
            allowedTypeChangedIntent.putExtra(EMRemoteActions.ACTION_EXTRA_ALLOWED_TYPE, allowedType);
            getApplication().startService(allowedTypeChangedIntent);
        }
    }

    /**
     * Sets the current playback index.  This should only be used when jumping
     * down the current playback list, if you are only changing one see {@link #next()} or
     * {@link #previous()}.
     *
     * @param index The index to become the current playback position.
     */
    public void setCurrentIndex(int index) {
        if (index >= getPlayListSize()) {
            index = getPlayListSize() - 1;
        }

        currentPosition = findNextAllowedIndex(index);
    }

    /**
     * Retrieves the current item index
     *
     * @return The current items index
     */
    public int getCurrentIndex() {
        return currentPosition;
    }

    /**
     * Attempts to find the index for the item with the specified itemId.  If no
     * such item exists then the current index will NOT be modified.  However if the item
     * is found then that index will be used to update the current index.  You can also
     * manually set the current index with {@link #setCurrentIndex(int)}.
     *
     * @param itemId The items id to use for finding the new index
     */
    public void setCurrentItem(long itemId) {
        if (playList == null) {
            return;
        }

        int index = getIndexForItem(itemId);
        if (index != INVALID_PLAYLIST_INDEX) {
            setCurrentIndex(index);
        }
    }

    /**
     * Determines the index for the item with the passed id.
     *
     * @param itemId The items id to use for finding the index
     * @return The items index or {@link #INVALID_PLAYLIST_INDEX}
     */
    public int getIndexForItem(long itemId) {
        if (playList == null) {
            return INVALID_PLAYLIST_INDEX;
        }

        int index = 0;
        for (I item : playList) {
            if (item.getId() == itemId) {
                return index;
            }

            index++;
        }

        return INVALID_PLAYLIST_INDEX;
    }

    /**
     * Determines if the given ItemQuery is the same as the current item
     *
     * @param item The ItemQuery to compare to the current item
     * @return True if the current item matches the passed item
     */
    public boolean isPlayingItem(I item) {
        I currentItem = getCurrentItem();

        //noinspection SimplifiableIfStatement
        if (item == null || currentItem == null) {
            return false;
        }

        return item.getId() == currentItem.getId() && item.getPlaylistId() == playListId;
    }

    /**
     * Determines if there is another item in the play list after the current one.
     *
     * @return True if there is an item after the current one
     */
    public boolean isNextAvailable() {
        return getPlayListSize() > findNextAllowedIndex(currentPosition + 1);
    }

    /**
     * Determines if there is an item in the play list before the current one.
     *
     * @return True if there is an item before the current one
     */
    public boolean isPreviousAvailable() {
        return findPreviousAllowedIndex(currentPosition - 1) != getPlayListSize();
    }

    /**
     * Returns the current playListId for this playlist.
     *
     * @return The playlist id [default: {@link #INVALID_PLAYLIST_ID}]
     */
    public long getPlayListId() {
        return playListId;
    }

    /**
     * Determines the current items type.  If the item doesn't exist or
     * isn't and audio or video item then {@link MediaType#NONE} will be returned.
     *
     * @return A {@link MediaType} representing the current items type
     */
    public MediaType getCurrentItemType() {
        I item = getCurrentItem();
        return item != null ? item.getMediaType() : MediaType.NONE;
    }

    /**
     * Returns the current size of the playlist.
     *
     * @return The size of the playlist
     */
    public int getPlayListSize() {
        return playList == null ? 0 : playList.size();
    }

    /**
     * Retrieves the item at the given index in the playlist.  If the playlist
     * is null or the index is out of bounds then null will be returned.
     *
     * @param index The index in the playlist to grab the item for
     * @return The retrieved item or null
     */
    @Nullable
    public I getItem(int index) {
        if (playList == null || index < 0 || index >= playList.size()) {
            return null;
        }

        return playList.get(index);
    }

    /**
     * Retrieves the Item representing the currently selected
     * item.  If there aren't any items in the play list then null will
     * be returned instead.
     *
     * @return The current Item or null
     */
    @Nullable
    public I getCurrentItem() {
        if (currentPosition < getPlayListSize()) {
            return getItem(currentPosition);
        }

        return null;
    }

    /**
     * Updates the currently selected item to the next one and retrieves the
     * Item representing that item.  If there aren't any items in the play
     * list or there isn't a next item then null will be returned.
     *
     * @return The next Item or null
     */
    @Nullable
    public I next() {
        currentPosition = findNextAllowedIndex(currentPosition + 1);
        return getCurrentItem();
    }

    /**
     * Updates the currently selected item to the previous one and retrieves the
     * Item representing that item.  If there aren't any items in the play
     * list or there isn't a previous item then null will be returned.
     *
     * @return The previous Item or null
     */
    @Nullable
    public I previous() {
        currentPosition = findPreviousAllowedIndex(currentPosition - 1);
        return getCurrentItem();
    }

    /**
     * Holds a weak reference to the LDSVideoView to use for playback events such as next or previous.
     *
     * @param videoView The LDSVideoView to use, or null
     */
    public void setVideoView(@Nullable EMVideoView videoView) {
        this.videoPlayer = new WeakReference<>(videoView);
    }

    @Nullable
    public EMVideoView getVideoView() {
        return videoPlayer.get();
    }

    /**
     * Informs the Media service that the current item
     * needs to be played/paused.  The service specified with
     * {@link #getMediaServiceClass()}} will be informed using the action
     * {@link EMRemoteActions#ACTION_PLAY_PAUSE}
     */
    public void invokePausePlay() {
        sendPendingIntent(playPausePendingIntent);
    }

    /**
     * Informs the Media service that we need to seek to
     * the next item. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_NEXT}
     */
    public void invokeNext() {
        sendPendingIntent(nextPendingIntent);
    }

    /**
     * Informs the Media service that we need to seek to
     * the previous item. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_PREVIOUS}
     */
    public void invokePrevious() {
        sendPendingIntent(previousPendingIntent);
    }

    /**
     * Informs the Media service that we need to stop
     * playback. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_STOP}
     */
    public void invokeStop() {
        sendPendingIntent(stopPendingIntent);
    }

    /**
     * Informs the Media service that we need to repeat
     * the current playback item. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_REPEAT}
     */
    public void invokeRepeat() {
        sendPendingIntent(repeatPendingIntent);
    }

    /**
     * Informs the Media service that we need to shuffle the
     * current playlist items. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_SHUFFLE}
     */
    public void invokeShuffle() {
        sendPendingIntent(shufflePendingIntent);
    }

    /**
     * Informs the Media service that we have started seeking
     * the playback.  The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_SEEK_STARTED}
     */
    public void invokeSeekStarted() {
        sendPendingIntent(seekStartedPendingIntent);
    }

    /**
     * Informs the Media service that we need to seek
     * the current item. The service specified with
     * {@link #getMediaServiceClass()} will be informed using the action
     * {@link EMRemoteActions#ACTION_SEEK_ENDED} and have an intent extra with the
     * key {@link EMRemoteActions#ACTION_EXTRA_SEEK_POSITION} (integer)
     */
    public void invokeSeekEnded(int seekPosition) {
        //Tries to start the intent
        if (seekEndedIntent != null) {
            seekEndedIntent.putExtra(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, seekPosition);
            getApplication().startService(seekEndedIntent);
        }
    }

    /**
     * Creates the Intents that will be used to interact with the playlist service
     *
     * @param mediaServiceClass The class to inform of any media playback controls
     * @param application The application to use when constructing the intents used to inform the playlist service of invocations
     */
    protected void constructControlIntents(Class<? extends Service> mediaServiceClass, Application application) {
        previousPendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_PREVIOUS);
        nextPendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_NEXT);
        playPausePendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_PLAY_PAUSE);
        repeatPendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_REPEAT);
        shufflePendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_SHUFFLE);

        stopPendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_STOP);
        seekStartedPendingIntent = createPendingIntent(application, mediaServiceClass, EMRemoteActions.ACTION_SEEK_STARTED);

        seekEndedIntent = new Intent(application, mediaServiceClass);
        seekEndedIntent.setAction(EMRemoteActions.ACTION_SEEK_ENDED);

        allowedTypeChangedIntent = new Intent(application, mediaServiceClass);
        allowedTypeChangedIntent.setAction(EMRemoteActions.ACTION_ALLOWED_TYPE_CHANGED);
    }

    /**
     * Finds the next item index that has an allowed type
     *
     * @param index The index to start with
     * @return The new index, or the list size if none exist
     */
    protected int findNextAllowedIndex(int index) {
        if (index >= getPlayListSize()) {
            return getPlayListSize();
        }

        if (index < 0) {
            index = 0;
        }

        while (index < getPlayListSize() && !isAllowedType(playList.get(index))) {
            index++;
        }

        return index < getPlayListSize() ? index : getPlayListSize();
    }

    /**
     * Finds the previous item index that has an allowed type
     *
     * @param index The index to start with
     * @return The new index, or the list size if none exist
     */
    protected int findPreviousAllowedIndex(int index) {
        if (index >= getPlayListSize() || index < 0) {
            return getPlayListSize();
        }

        while (index >= 0 && !isAllowedType(playList.get(index))) {
            index--;
        }

        return index >= 0 ? index : getPlayListSize();
    }

    /**
     * Determines if the passed item is of the correct type to allow playback
     *
     * @param item The item to determine if it is allowed
     * @return True if the item is null or is allowed
     */
    protected boolean isAllowedType(@Nullable I item) {
        if (item == null || item.getMediaType() == MediaType.NONE) {
            return false;
        }

        if (allowedType == MediaType.AUDIO_AND_VIDEO) {
            return item.getMediaType() == MediaType.AUDIO || item.getMediaType() == MediaType.VIDEO;
        }

        return allowedType == item.getMediaType();
    }

    /**
     * Creates a PendingIntent for the given action to the specified service
     *
     * @param application The application to use when creating the  pending intent
     * @param serviceClass The service class to notify of intents
     * @param action The action to use
     * @return The resulting PendingIntent
     */
    protected PendingIntent createPendingIntent(Application application, Class<? extends Service> serviceClass, String action) {
        Intent intent = new Intent(application, serviceClass);
        intent.setAction(action);

        return PendingIntent.getService(application, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Attempts to send the specified PendingIntent.
     *
     * @param pi The pending intent to send
     */
    protected void sendPendingIntent(PendingIntent pi) {
        if (pi == null) {
            return;
        }

        try {
            pi.send();
        } catch (Exception e) {
            Log.d(TAG, "Error sending pending intent " + pi.toString(), e);
        }
    }
}
