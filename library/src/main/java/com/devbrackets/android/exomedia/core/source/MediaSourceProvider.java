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
@SuppressWarnings("WeakerAccess")
public class MediaSourceProvider {
    protected static final String USER_AGENT_FORMAT = "ExoMedia %s (%d) / Android %s / %s";

    @NonNull
    @SuppressLint("DefaultLocale")
    protected String userAgent = String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.RELEASE, Build.MODEL);

    @NonNull
    public MediaSource generate(@NonNull Context context, @NonNull Handler handler, @NonNull Uri uri, @Nullable TransferListener<? super DataSource> transferListener ) {
        SourceTypeBuilder sourceTypeBuilder = findByProviders(uri);

        // If a registered builder wasn't found then use the default
        MediaSourceBuilder builder = sourceTypeBuilder != null ? sourceTypeBuilder.builder : new DefaultMediaSourceBuilder();
        return builder.build(context, uri, userAgent, handler, transferListener);
    }

    @Nullable
    protected static SourceTypeBuilder findByProviders(@NonNull Uri uri) {
        // Uri Scheme (e.g. rtsp)
        SourceTypeBuilder sourceTypeBuilder = findByScheme(uri);
        if (sourceTypeBuilder != null) {
            return sourceTypeBuilder;
        }

        // Extension
        sourceTypeBuilder = findByExtension(uri);
        if (sourceTypeBuilder != null) {
            return sourceTypeBuilder;
        }

        // Regex
        sourceTypeBuilder = findByLooseComparison(uri);
        if (sourceTypeBuilder != null) {
             return sourceTypeBuilder;
        }

        return null;
    }

    @Nullable
    protected static SourceTypeBuilder findByScheme(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isEmpty()) {
            return null;
        }

        for (SourceTypeBuilder builder : ExoMedia.Data.sourceTypeBuilders) {
            if (builder.uriScheme != null && builder.uriScheme.equalsIgnoreCase(scheme)) {
                return builder;
            }
        }

        return null;
    }

    @Nullable
    protected static SourceTypeBuilder findByExtension(@NonNull Uri uri) {
        String extension = MediaSourceUtil.getExtension(uri);
        if (extension == null || extension.isEmpty()) {
            return null;
        }

        for (SourceTypeBuilder builder : ExoMedia.Data.sourceTypeBuilders) {
            if (builder.extension != null && builder.extension.equalsIgnoreCase(extension)) {
                return builder;
            }
        }

        return null;
    }

    @Nullable
    protected static SourceTypeBuilder findByLooseComparison(@NonNull Uri uri) {
        for (SourceTypeBuilder builder : ExoMedia.Data.sourceTypeBuilders) {
            if (builder.looseComparisonRegex != null && uri.toString().matches(builder.looseComparisonRegex)) {
                return builder;
            }
        }

        return null;
    }

    public static class SourceTypeBuilder {
        @NonNull
        public final MediaSourceBuilder builder;
        @Nullable
        public final String extension;
        @Nullable
        public final String uriScheme;
        @Nullable
        public final String looseComparisonRegex;

        /**
         * @deprecated Use {@link #SourceTypeBuilder(MediaSourceBuilder, String, String, String)}
         */
        @Deprecated
        public SourceTypeBuilder(@NonNull MediaSourceBuilder builder, @NonNull String extension, @Nullable String looseComparisonRegex) {
            this(builder, null, extension, looseComparisonRegex);
        }

        public SourceTypeBuilder(@NonNull MediaSourceBuilder builder, @Nullable String uriScheme, @Nullable String extension, @Nullable String looseComparisonRegex) {
            this.builder = builder;
            this.uriScheme = uriScheme;
            this.extension = extension;
            this.looseComparisonRegex = looseComparisonRegex;
        }
    }
}
