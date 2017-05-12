package com.devbrackets.android.exomedia.core.source;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.source.builder.DefaultMediaSourceBuilder;
import com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder;
import com.devbrackets.android.exomedia.util.MediaSourceUtil;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

/**
 * Provides the functionality to determine which {@link MediaSource} should be used
 * to play a particular URL.
 */
public class MediaSourceProvider {
    protected static final String USER_AGENT_FORMAT = "ExoMedia %s (%d) / Android %s / %s";

    @NonNull
    @SuppressLint("DefaultLocale")
    protected String userAgent = String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.RELEASE, Build.MODEL);

    @NonNull
    public MediaSource generate(@NonNull Context context, @NonNull Handler handler, @NonNull Uri uri, @Nullable TransferListener<? super DataSource> transferListener ) {
        String extension = MediaSourceUtil.getExtension(uri);

        // Searches for a registered builder
        SourceTypeBuilder sourceTypeBuilder = findByExtension(extension);
        if (sourceTypeBuilder == null) {
            sourceTypeBuilder = findByLooseComparison(uri);
        }

        // If a registered builder wasn't found then use the default
        MediaSourceBuilder builder = sourceTypeBuilder != null ? sourceTypeBuilder.builder : new DefaultMediaSourceBuilder();
        return builder.build(context, uri, userAgent, handler, transferListener);
    }

    @Nullable
    protected static SourceTypeBuilder findByExtension(@Nullable String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }

        for (SourceTypeBuilder sourceTypeBuilder : ExoMedia.Data.sourceTypeBuilders) {
            if (sourceTypeBuilder.extension.equalsIgnoreCase(extension)) {
                return sourceTypeBuilder;
            }
        }

        return null;
    }

    @Nullable
    protected static SourceTypeBuilder findByLooseComparison(@NonNull Uri uri) {
        for (SourceTypeBuilder sourceTypeBuilder : ExoMedia.Data.sourceTypeBuilders) {
            if (sourceTypeBuilder.looseComparisonRegex != null && uri.toString().matches(sourceTypeBuilder.looseComparisonRegex)) {
                return sourceTypeBuilder;
            }
        }

        return null;
    }

    public static class SourceTypeBuilder {
        @NonNull
        public final MediaSourceBuilder builder;
        @NonNull
        public final String extension;
        @Nullable
        public final String looseComparisonRegex;

        public SourceTypeBuilder(@NonNull MediaSourceBuilder builder, @NonNull String extension, @Nullable String looseComparisonRegex) {
            this.builder = builder;
            this.extension = extension;
            this.looseComparisonRegex = looseComparisonRegex;
        }
    }
}
