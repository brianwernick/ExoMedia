/*
 * Copyright (C) 2016 Brian Wernick
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

package com.devbrackets.android.exomedia;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.service.EMPlaylistService;

/**
 * An object to hold the information necessary to populate a notification
 *
 * @deprecated The {@link EMPlaylistService}, {@link EMPlaylistManager}, and associated helpers will be removed in the next
 * major release (3.0).  Instead the Playlist functionality has been extracted in to a separate library called
 * <a href="https://github.com/brianwernick/PlaylistCore">PlaylistCore</a>
 */
@Deprecated
class NotificationInfo {
    @Nullable
    private String title;
    @Nullable
    private String album;
    @Nullable
    private String artist;

    @Nullable
    private Bitmap largeImage;
    @Nullable
    private Bitmap secondaryImage;

    @DrawableRes
    private int appIcon;
    private int notificationId;

    private boolean showNotifications;

    private PendingIntent pendingIntent;

    private EMNotification.NotificationMediaState mediaState;

    public void clean() {
        appIcon = 0;
        notificationId = 0;

        title = null;
        album = null;
        artist = null;

        largeImage = null;
        secondaryImage = null;
        pendingIntent = null;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public void setAlbum(@Nullable String album) {
        this.album = album;
    }

    public void setArtist(@Nullable String artist) {
        this.artist = artist;
    }

    public void setLargeImage(@Nullable Bitmap largeImage) {
        this.largeImage = largeImage;
    }

    public void setSecondaryImage(@Nullable Bitmap secondaryImage) {
        this.secondaryImage = secondaryImage;
    }

    public void setAppIcon(@DrawableRes int appIcon) {
        this.appIcon = appIcon;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public void setShowNotifications(boolean showNotifications) {
        this.showNotifications = showNotifications;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public void setMediaState(@Nullable EMNotification.NotificationMediaState mediaState) {
        this.mediaState = mediaState;
    }

    @NonNull
    public String getTitle() {
        return title != null ? title : "";
    }

    @NonNull
    public String getAlbum() {
        return album != null ? album : "";
    }

    @NonNull
    public String getArtist() {
        return artist != null ? artist : "";
    }

    @Nullable
    public Bitmap getLargeImage() {
        return largeImage;
    }

    @Nullable
    public Bitmap getSecondaryImage() {
        return secondaryImage;
    }

    @DrawableRes
    public int getAppIcon() {
        return appIcon;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public boolean getShowNotifications() {
        return showNotifications;
    }

    @Nullable
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Nullable
    public EMNotification.NotificationMediaState getMediaState() {
        return mediaState;
    }
}
