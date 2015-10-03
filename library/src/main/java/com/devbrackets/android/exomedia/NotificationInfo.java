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

package com.devbrackets.android.exomedia;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An object to hold the information necessary to populate a notification
 */
class NotificationInfo {
    private String title;
    private String album;
    private String artist;

    private Bitmap largeImage;
    private Bitmap secondaryImage;

    @DrawableRes
    private int appIcon;
    private int notificationId;

    private boolean showNotifications;

    private PendingIntent pendingIntent;

    private EMNotification.NotificationMediaState mediaState;

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
