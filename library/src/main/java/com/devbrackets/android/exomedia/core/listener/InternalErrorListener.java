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

package com.devbrackets.android.exomedia.core.listener;

import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer;

/**
 * A listener for internal errors.
 * <p>
 * These errors are not visible to the user, and hence this listener is provided for
 * informational purposes only. Note however that an internal error may cause a fatal
 * error if the player fails to recover. If this happens, {@link ExoPlayerListener#onError(ExoMediaPlayer, Exception)}
 * will be invoked.
 */
public interface InternalErrorListener {
    void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs);

    void onDrmSessionManagerError(Exception e);
}
