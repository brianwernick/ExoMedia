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
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.TransferListener;

public class DashMediaSourceBuilder extends MediaSourceBuilder {
    @NonNull
    @Override
    public MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener<? super DataSource> transferListener) {
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(context, userAgent, transferListener);
        DataSource.Factory meteredDataSourceFactory = buildDataSourceFactory(context, userAgent, new DefaultBandwidthMeter());

        return new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(meteredDataSourceFactory), handler, null);
    }
}
