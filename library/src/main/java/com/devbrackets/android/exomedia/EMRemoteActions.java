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

public class EMRemoteActions {
    public static final String ACTION_START_SERVICE = "remote_action_start_service";

    public static final String ACTION_PLAY_PAUSE = "remote_action_play_pause";
    public static final String ACTION_PREVIOUS = "remote_action_previous";
    public static final String ACTION_NEXT = "remote_action_next";
    public static final String ACTION_STOP = "remote_action_stop";
    public static final String ACTION_REPEAT = "remote_action_repeat";
    public static final String ACTION_SHUFFLE = "remote_action_shuffle";

    public static final String ACTION_SEEK_STARTED = "remote_action_seek_started";
    public static final String ACTION_SEEK_ENDED = "remote_action_seek_ended";

    public static final String ACTION_ALLOWED_TYPE_CHANGED = "remote_action_allowed_type_changed";

    //Extras
    public static final String ACTION_EXTRA_SEEK_POSITION = "remote_action_seek_position";
    public static final String ACTION_EXTRA_ALLOWED_TYPE = "remote_action_allowed_type";
    public static final String ACTION_EXTRA_START_PAUSED = "remote_action_start_paused";

    private EMRemoteActions() {
        //Purposefully left blank
    }
}
