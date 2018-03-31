package com.devbrackets.android.exomedia;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.core.source.MediaSourceProvider;
import com.devbrackets.android.exomedia.core.source.builder.DashMediaSourceBuilder;
import com.devbrackets.android.exomedia.core.source.builder.HlsMediaSourceBuilder;
import com.devbrackets.android.exomedia.core.source.builder.SsMediaSourceBuilder;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A standard entry point for registering additional {@link com.google.android.exoplayer2.Renderer}s and
 * {@link com.google.android.exoplayer2.source.MediaSource}s
 */
public class ExoMedia {
    /**
     * @deprecated Use {@link DataSourceFactoryProvider} instead
     */
    @Deprecated
    public interface HttpDataSourceFactoryProvider {
        @NonNull
        HttpDataSource.BaseFactory provide(@NonNull String userAgent, @Nullable TransferListener<? super DataSource> listener);
    }

    public interface DataSourceFactoryProvider {
        @NonNull
        DataSource.Factory provide(@NonNull String userAgent, @Nullable TransferListener<? super DataSource> listener);
    }

    public enum RendererType {
        AUDIO,
        VIDEO,
        CLOSED_CAPTION,
        METADATA
    }

    /**
     * Registers additional customized {@link com.google.android.exoplayer2.Renderer}s
     * that will be used by the {@link com.google.android.exoplayer2.source.MediaSource}s to
     * correctly play media.
     *
     * @param type The type for the renderer
     * @param clazz The class of the customized Renderer
     */
    public static void registerRenderer(@NonNull RendererType type, @NonNull Class<? extends Renderer> clazz) {
        Data.registeredRendererClasses.get(type).add(clazz.getName());
    }

    /**
     * Registers additional {@link com.google.android.exoplayer2.source.MediaSource}s for the specified file
     * extensions (and regexes). {@link com.google.android.exoplayer2.source.MediaSource}s registered here will take
     * precedence to the pre-configured ones.
     *
     * @param builder The builder for additional or customized media sources
     */
    public static void registerMediaSourceBuilder(@NonNull MediaSourceProvider.SourceTypeBuilder builder) {
        Data.sourceTypeBuilders.add(0, builder);
    }

    /**
     * Specifies the provider to use when building {@link com.google.android.exoplayer2.upstream.HttpDataSource.BaseFactory}
     * instances for use with the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder}s. This will
     * only be used for builders that haven't customized the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder#buildDataSourceFactory(Context, String, TransferListener)}
     * method.
     *
     * @param provider The provider to use for the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder}s
     * @deprecated Use {@link #setDataSourceFactoryProvider(DataSourceFactoryProvider)} instead as it is more permissive
     */
    @Deprecated
    public static void setHttpDataSourceFactoryProvider(@Nullable HttpDataSourceFactoryProvider provider) {
        Data.httpDataSourceFactoryProvider = provider;
    }

    /**
     * Specifies the provider to use when building {@link com.google.android.exoplayer2.upstream.DataSource.Factory}
     * instances for use with the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder}s. This will
     * only be used for builders that haven't customized the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder#buildDataSourceFactory(Context, String, TransferListener)}
     * method.
     *
     * @param provider The provider to use for the {@link com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder}s
     */
    public static void setDataSourceFactoryProvider(@Nullable DataSourceFactoryProvider provider) {
        Data.dataSourceFactoryProvider = provider;
    }

    /**
     * Specifies the {@link LoadControl} to use when building the {@link com.google.android.exoplayer2.ExoPlayer} instance
     * used in the {@link com.devbrackets.android.exomedia.ui.widget.VideoView} and {@link AudioPlayer}. This allows the
     * buffering amounts to be modified to better suit your needs which can be easily specified by using an instance of
     * {@link com.google.android.exoplayer2.DefaultLoadControl}. When the <code>loadControl</code> is <code>null</code>
     * the default instance of the {@link com.google.android.exoplayer2.DefaultLoadControl} will be used. This will only
     * take effect for any instances created <i>after</i> this was set.
     *
     * @param loadControl The {@link LoadControl} to use for any new {@link com.google.android.exoplayer2.ExoPlayer} instances
     */
    public static void setLoadControl(@Nullable LoadControl loadControl) {
        Data.loadControl = loadControl;
    }

    public static class Data {
        @NonNull
        public static final Map<RendererType, List<String>> registeredRendererClasses = new HashMap<>();
        @NonNull
        public static final List<MediaSourceProvider.SourceTypeBuilder> sourceTypeBuilders = new ArrayList<>();
        @Nullable
        @Deprecated
        public static volatile HttpDataSourceFactoryProvider httpDataSourceFactoryProvider;
        @Nullable
        public static volatile DataSourceFactoryProvider dataSourceFactoryProvider;
        @Nullable
        public static volatile LoadControl loadControl;

        static {
            instantiateRendererClasses();
            instantiateSourceProviders();
        }

        private static void instantiateRendererClasses() {
            // Instantiates the required values
            registeredRendererClasses.put(RendererType.AUDIO, new LinkedList<String>());
            registeredRendererClasses.put(RendererType.VIDEO, new LinkedList<String>());
            registeredRendererClasses.put(RendererType.CLOSED_CAPTION, new LinkedList<String>());
            registeredRendererClasses.put(RendererType.METADATA, new LinkedList<String>());

            // Adds the ExoPlayer extension library renderers
            List<String> audioClasses = registeredRendererClasses.get(RendererType.AUDIO);
            audioClasses.add("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer");
            audioClasses.add("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer");
            audioClasses.add("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer");

            List<String> videoClasses = registeredRendererClasses.get(RendererType.VIDEO);
            videoClasses.add("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer");
        }

        private static void instantiateSourceProviders() {
            // Adds the HLS, SmoothStream, and MPEG Dash registrations
            sourceTypeBuilders.add(new MediaSourceProvider.SourceTypeBuilder(new HlsMediaSourceBuilder(), ".m3u8", ".*\\.m3u8.*"));
            sourceTypeBuilders.add(new MediaSourceProvider.SourceTypeBuilder(new DashMediaSourceBuilder(), ".mpd", ".*\\.mpd.*"));
            sourceTypeBuilders.add(new MediaSourceProvider.SourceTypeBuilder(new SsMediaSourceBuilder(), ".ism", ".*\\.ism.*"));
        }
    }
}
