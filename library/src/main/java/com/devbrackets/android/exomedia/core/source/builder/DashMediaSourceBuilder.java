package com.devbrackets.android.exomedia.core.source.builder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

public class DashMediaSourceBuilder extends MediaSourceBuilder {
    @NonNull
    @Override
    public MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener<? super DataSource> transferListener) {
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(context, userAgent, null);
        DataSource.Factory meteredDataSourceFactory = buildDataSourceFactory(context, userAgent, transferListener);

        return new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(meteredDataSourceFactory), dataSourceFactory)
                .createMediaSource(uri, handler, null);
    }
}
