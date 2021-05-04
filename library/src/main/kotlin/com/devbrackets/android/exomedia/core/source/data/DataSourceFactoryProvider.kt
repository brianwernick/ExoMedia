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

package com.devbrackets.android.exomedia.core.source.data

import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * A Provider for [DataSource.Factory] instances. This is useful
 * for specifying custom data source backing instances such as
 * using OKHTTP instead of the included Android http clients
 */
interface DataSourceFactoryProvider {

  /**
   * Provides a [DataSource.Factory] to use when requesting network
   * data to load media.
   *
   * @param userAgent The "UserAgent" to use when making network requests
   * @param listener A [TransferListener] to inform of data load & transfer events
   * @return An instance of [DataSource.Factory]
   */
  fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory
}