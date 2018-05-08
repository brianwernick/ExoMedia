/*
 * Copyright (C) 2018 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.exoplayer;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.Timeline;

public class WindowInfo {
    public final int previousWindowIndex;
    public final int currentWindowIndex;
    public final int nextWindowIndex;

    @NonNull
    public final Timeline.Window currentWindow;

    public WindowInfo(int previousWindowIndex, int currentWindowIndex, int nextWindowIndex, @NonNull Timeline.Window currentWindow) {
        this.previousWindowIndex = previousWindowIndex;
        this.currentWindowIndex = currentWindowIndex;
        this.nextWindowIndex = nextWindowIndex;
        this.currentWindow = currentWindow;
    }
}