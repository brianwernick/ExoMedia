package com.devbrackets.android.exomedia.core.source.builder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.TransferListener;

public class SsMediaSourceBuilder extends MediaSourceBuilder {
    @NonNull
    @Override
    public MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener<? super DataSource> transferListener) {
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(context, userAgent, transferListener);
        DataSource.Factory meteredDataSourceFactory = buildDataSourceFactory(context, userAgent, new DefaultBandwidthMeter());

        return new SsMediaSource(uri, dataSourceFactory, new DefaultSsChunkSource.Factory(meteredDataSourceFactory), handler, null);
    }
}
