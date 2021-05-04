/*
 * Copyright (C) 2017 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.nmp.manager.track

data class RendererTrackInfo(
    /**
     * The exo player renderer track indexes
     */
    val indexes: List<Int>,

    /**
     * The renderer track index related to the requested `groupIndex`
     */
    val index: Int,

    /**
     * The corrected exoplayer group index which may be used to obtain proper track group from the renderer
     */
    val groupIndex: Int
)