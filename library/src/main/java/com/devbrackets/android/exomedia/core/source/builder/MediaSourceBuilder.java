/*
 * Copyright (C) 2017 - 2018 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.source.builder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.ExoMedia;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

public abstract class MediaSourceBuilder {

    @NonNull
    public abstract MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener transferListener);

    @NonNull
    protected DataSource.Factory buildDataSourceFactory(@NonNull Context context, @NonNull String userAgent, @Nullable TransferListener listener) {
        ExoMedia.DataSourceFactoryProvider provider = ExoMedia.Data.dataSourceFactoryProvider;
        DataSource.Factory dataSourceFactory = provider != null ? provider.provide(userAgent, listener) : null;

        // Handles the deprecated httpDataSourceFactoryProvider
        if (dataSourceFactory == null) {
            ExoMedia.HttpDataSourceFactoryProvider httpProvider = ExoMedia.Data.httpDataSourceFactoryProvider;
            dataSourceFactory = httpProvider != null ? httpProvider.provide(userAgent, listener) : null;
        }

        // If no factory was provided use the default one
        if (dataSourceFactory == null) {
            dataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, listener);
        }

        return new DefaultDataSourceFactory(context, listener, dataSourceFactory);
    }
}
