package com.devbrackets.android.exomedia.core.source.builder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.util.SubtitleUtil;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

public class DefaultMediaSourceBuilder extends MediaSourceBuilder {
    @NonNull
    @Override
    public MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener<? super DataSource> transferListener) {
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(context, userAgent, transferListener);

        Uri captionUri = Uri.parse("http://www.maxovahra.com/TAP_MAXSGAME_SUBS_GR.srt");

        ExtractorMediaSource mediaSource = new ExtractorMediaSource(
                uri,
                dataSourceFactory,
                new DefaultExtractorsFactory(),
                handler,
                null
        );

        return captionUri == null
                ? mediaSource
                : SubtitleUtil.createMergingMediaSource(
                dataSourceFactory,
                mediaSource,
                captionUri
        );
    }


}
