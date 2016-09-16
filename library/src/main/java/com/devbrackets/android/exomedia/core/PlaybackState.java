/*
 * Copyright (C) 2016 Brian Wernick,
 * Copyright (C) 2014 The Android Open Source Project
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



package com.devbrackets.android.exomedia.core;

import android.support.annotation.IntDef;

import com.google.android.exoplayer.ExoPlayer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ExoPlayer.STATE_IDLE, ExoPlayer.STATE_PREPARING, ExoPlayer.STATE_READY, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_ENDED})
public @interface PlaybackState {}
