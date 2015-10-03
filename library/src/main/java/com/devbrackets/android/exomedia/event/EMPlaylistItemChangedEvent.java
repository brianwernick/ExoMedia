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

package com.devbrackets.android.exomedia.event;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class EMPlaylistItemChangedEvent<T extends EMPlaylistManager.PlaylistItem> {
    private final T currentItem;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public EMPlaylistItemChangedEvent(T currentItem, boolean hasPrevious, boolean hasNext) {
        this.currentItem = currentItem;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    public T getCurrentItem() {
        return currentItem;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }
}
