package com.devbrackets.android.exomedia.manager;

import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.EMRemoteActions;
import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.exomedia.event.EMAllowedMediaTypeChangedEvent;
import com.devbrackets.android.exomedia.event.EMMediaNextEvent;
import com.devbrackets.android.exomedia.event.EMMediaPlayPauseEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreviousEvent;
import com.devbrackets.android.exomedia.event.EMMediaSeekEndedEvent;
import com.devbrackets.android.exomedia.event.EMMediaStopEvent;
import com.squareup.otto.Bus;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A Manager to keep track of play lists that can contain audio or video items
 */
@SuppressWarnings("unused")
public abstract class EMPlaylistManager<T extends EMPlaylistManager.PlaylistItem> {
    private static final String TAG = "EMPlaylistManager";
    public static final String ACTION_PLAY = "com.devbrackets.android.exomedia.action.PLAY";
    public static final String EXTRA_SEEK_POSITION = "EXTRA_SEEK_POSITION";
    public static final String EXTRA_START_PAUSED = "EXTRA_START_PAUSED";

    public enum MediaType {
        AUDIO,
        VIDEO,
        AUDIO_AND_VIDEO,
        NONE
    }

    public interface PlaylistItem {
        long getId();
        long getPlaylistId();
        boolean isAudio();
        boolean isVideo();
        String getMediaUrl();
        String getTitle();
        String getThumbnailUrl();
        String getArtworkUrl();
    }

    private List<T> playList;
    private int currentPosition = 0;
    private long playListId = -1;
    private boolean isUserDefined;

    private MediaType allowedType = MediaType.AUDIO;
    private WeakReference<EMVideoView> videoPlayer = new WeakReference<>(null);

    @Nullable
    public abstract Bus getBus();
    public abstract Application getApplication();
    public abstract Class<? extends Service> getMediaServiceClass();

    @Nullable
    private PendingIntent playPausePendingIntent, nextPendingIntent, previousPendingIntent, stopPendingIntent;
    @Nullable
    private Intent seekIntent;

    public void play(List<T> playListItems, int startIndex, int seekPosition, boolean startPaused) {
        setParameters(playListItems, startIndex);
        play(seekPosition, startPaused);
    }

    /**
     * In order to use this method you must call {@link #setParameters(java.util.List, int)} first.
     * Alternatively you can call {@link #play(java.util.List, int, int, boolean)}
     */
    public void play(int seekPosition, boolean startPaused) {
        T currentItem = getCurrentItem();

        if (currentItem == null) {
            return;
        }

        //Starts the playlist service
        Intent intent = new Intent(getApplication(), getMediaServiceClass());
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_SEEK_POSITION, seekPosition);
        intent.putExtra(EXTRA_START_PAUSED, startPaused);
        getApplication().startService(intent);
    }

    /**
     * Sets the List of items to be used for the play list.  This can include both audio
     * and video items.
     *
     * @param playListItems The List of items to play
     * @param startIndex    The index in the list to start playback with
     */
    public void setParameters(List<T> playListItems, int startIndex) {
        playList = playListItems;

        setCurrentIndex(startIndex);
        setPlayListInfo(-1, false);
    }

    /**
     * Sets the information for the current playlist.  This will only last until the
     * playListItems are swapped out.
     *
     * @param playListId    The id for the playlist (or collection)
     * @param isUserDefined True if this is a user defined playlist (false for collections)
     */
    public void setPlayListInfo(long playListId, boolean isUserDefined) {
        this.playListId = playListId;
        this.isUserDefined = isUserDefined;
    }

    /**
     * Sets the class to inform of invoked media playback controls using intents.  These
     * will be one of:
     * <ul>
     * <li>{@link EMRemoteActions#ACTION_STOP}</li>
     * <li>{@link EMRemoteActions#ACTION_PLAY_PAUSE}</li>
     * <li>{@link EMRemoteActions#ACTION_PREVIOUS}</li>
     * <li>{@link EMRemoteActions#ACTION_NEXT}</li>
     * <li>{@link EMRemoteActions#ACTION_SEEK}</li>
     * </ul>
     * <p/>
     * <b><em>NOTE:</em></b> if you have specified a Bus with {@link #getBus()} then you don't
     * need to set the mediaServiceClass
     *
     * @param mediaServiceClass The class to inform of any media playback controls
     */
    public void setMediaServiceClass(@Nullable Class<? extends Service> mediaServiceClass) {
        if (mediaServiceClass == null) {
            nextPendingIntent = null;
            previousPendingIntent = null;
            playPausePendingIntent = null;
            stopPendingIntent = null;
            seekIntent = null;
            return;
        }

        //Creates the pending intents
        previousPendingIntent = createPendingIntent(EMRemoteActions.ACTION_PREVIOUS, mediaServiceClass);
        nextPendingIntent = createPendingIntent(EMRemoteActions.ACTION_NEXT, mediaServiceClass);
        playPausePendingIntent = createPendingIntent(EMRemoteActions.ACTION_PLAY_PAUSE, mediaServiceClass);

        stopPendingIntent = createPendingIntent(EMRemoteActions.ACTION_STOP, mediaServiceClass);

        seekIntent = new Intent(getApplication(), mediaServiceClass);
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

        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMAllowedMediaTypeChangedEvent(allowedType));
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
        if (index != -1) {
            setCurrentIndex(index);
        }
    }

    /**
     * Determines the index for the item with the passed id.
     *
     * @param itemId The items id to use for finding the index
     * @return The items index or -1
     */
    public int getIndexForItem(long itemId) {
        if (playList == null) {
            return -1;
        }

        int index = 0;
        for (T item : playList) {
            if (item.getId() == itemId) {
                return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * Determines if the given ItemQuery is the same as the current item
     *
     * @param item The ItemQuery to compare to the current item
     * @return True if the current item matches the passed item
     */
    public boolean isPlayingItem(T item) {
        T currentItem = getCurrentItem();

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
     * @return The playlist id [default: -1]
     */
    public long getPlayListId() {
        return playListId;
    }

    /**
     * Determines if the current playlist is a user defined playlist or a
     * collection.
     *
     * @return True if the current playlist is user defined [default: false]
     */
    public boolean isUserDefined() {
        return isUserDefined;
    }

    /**
     * Determines the current items type.  If the item doesn't exist or
     * isn't and audio or video item then {@link MediaType#NONE} will be returned.
     *
     * @return A {@link MediaType} representing the current items type
     */
    public MediaType getCurrentItemType() {
        T item = getCurrentItem();

        if (item != null) {
            if (item.isAudio()) {
                return MediaType.AUDIO;
            } else if (item.isVideo()) {
                return MediaType.VIDEO;
            }
        }

        return MediaType.NONE;
    }

    /**
     * Retrieves the Item representing the currently selected
     * item.  If there aren't any items in the play list then null will
     * be returned instead.
     *
     * @return The current Item or null
     */
    @Nullable
    public T getCurrentItem() {
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
    public T next() {
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
    public T previous() {
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
     * needs to be played/paused.  If {@link #getBus()} doesn't return
     * a null value then the service will be informed though the bus event
     * {@link EMMediaPlayPauseEvent}.  Otherwise the service specified with
     * {@link #setMediaServiceClass(Class)} will be informed using the action
     * {@link EMRemoteActions#ACTION_PLAY_PAUSE}
     */
    public void invokePausePlay() {
        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaPlayPauseEvent());
            return;
        }

        sendPendingIntent(playPausePendingIntent);
    }

    /**
     * Informs the Media service that we need to seek to
     * the next item.  If {@link #getBus()} doesn't return
     * a null value then the service will be informed though the bus event
     * {@link EMMediaNextEvent}.  Otherwise the service specified with
     * {@link #setMediaServiceClass(Class)} will be informed using the action
     * {@link EMRemoteActions#ACTION_NEXT}
     */
    public void invokeNext() {
        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaNextEvent());
            return;
        }

        sendPendingIntent(nextPendingIntent);
    }

    /**
     * Informs the Media service that we need to seek to
     * the previous item.  If {@link #getBus()} doesn't return
     * a null value then the service will be informed though the bus event
     * {@link EMMediaPreviousEvent}.  Otherwise the service specified with
     * {@link #setMediaServiceClass(Class)} will be informed using the action
     * {@link EMRemoteActions#ACTION_PREVIOUS}
     */
    public void invokePrevious() {
        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaPreviousEvent());
            return;
        }

        sendPendingIntent(previousPendingIntent);
    }

    /**
     * Informs the Media service that we need to stop
     * playback. If {@link #getBus()} doesn't return
     * a null value then the service will be informed though the bus event
     * {@link EMMediaStopEvent}.  Otherwise the service specified with
     * {@link #setMediaServiceClass(Class)} will be informed using the action
     * {@link EMRemoteActions#ACTION_STOP}
     */
    public void invokeStop() {
        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaStopEvent());
            return;
        }

        sendPendingIntent(stopPendingIntent);
    }

    /**
     * Informs the Media service that we need to seek
     * the current item. If {@link #getBus()} doesn't return
     * a null value then the service will be informed though the bus event
     * {@link EMMediaSeekEndedEvent}.  Otherwise the service specified with
     * {@link #setMediaServiceClass(Class)} will be informed using the action
     * {@link EMRemoteActions#ACTION_SEEK} and have an intent extra with the
     * key {@link EMRemoteActions#ACTION_EXTRA_SEEK_POSITION} (integer)
     */
    public void invokeSeek(int seekPosition) {
        Bus bus = getBus();
        if (bus != null) {
            bus.post(new EMMediaSeekEndedEvent(seekPosition));
            return;
        }

        //Tries to start the intent
        if (seekIntent != null) {
            seekIntent.putExtra(EMRemoteActions.ACTION_EXTRA_SEEK_POSITION, seekPosition);
            getApplication().startService(seekIntent);
        }
    }

    /**
     * Returns the current size of the playlist.
     *
     * @return The size of the playlist
     */
    private int getPlayListSize() {
        return playList == null ? 0 : playList.size();
    }

    /**
     * Finds the next item index that has an allowed type
     *
     * @param index The index to start with
     * @return The new index, or the list size if none exist
     */
    private int findNextAllowedIndex(int index) {
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
    private int findPreviousAllowedIndex(int index) {
        if (index >= getPlayListSize() || index < 0) {
            return getPlayListSize();
        }

        while (index >= 0 && !isAllowedType(playList.get(index))) {
            index--;
        }

        return index >= 0 ? index : getPlayListSize();
    }

    @Nullable
    private T getItem(int index) {
        if (playList == null) {
            return null;
        }

        return playList.get(index);
    }

    /**
     * Determines if the passed item is of the correct type to allow playback
     *
     * @param item The item to determine if it is allowed
     * @return True if the item is null or is allowed
     */
    private boolean isAllowedType(T item) {
        if (item == null) {
            return true;
        }

        switch (allowedType) {
            case AUDIO:
                return item.isAudio();

            case VIDEO:
                return item.isVideo();

            case AUDIO_AND_VIDEO:
                return item.isAudio() || item.isVideo();

            default:
            case NONE:
                return false;
        }
    }

    /**
     * Creates a PendingIntent for the given action to the specified service
     *
     * @param action The action to use
     * @param serviceClass The service class to notify of intents
     * @return The resulting PendingIntent
     */
    private PendingIntent createPendingIntent(String action, Class<? extends Service> serviceClass) {
        Intent intent = new Intent(getApplication(), serviceClass);
        intent.setAction(action);

        return PendingIntent.getService(getApplication(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Attempts to send the specified PendingIntent.
     *
     * @param pi The pending intent to send
     */
    private void sendPendingIntent(PendingIntent pi) {
        if (pi == null) {
            return;
        }

        try {
            pi.send();
        } catch (Exception e) {
            Log.d(TAG, "Error sending lock screen pending intent", e);
        }
    }
}
