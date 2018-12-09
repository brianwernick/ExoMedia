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

package com.devbrackets.android.exomedia

import com.devbrackets.android.exomedia.core.source.MediaSourceProvider
import com.devbrackets.android.exomedia.core.source.builder.DashMediaSourceBuilder
import com.devbrackets.android.exomedia.core.source.builder.HlsMediaSourceBuilder
import com.devbrackets.android.exomedia.core.source.builder.SsMediaSourceBuilder
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import java.util.*

/**
 * A standard entry point for registering additional [com.google.android.exoplayer2.Renderer]s and
 * [com.google.android.exoplayer2.source.MediaSource]s
 */
object ExoMedia {
    interface DataSourceFactoryProvider {
        fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory
    }

    enum class RendererType {
        AUDIO,
        VIDEO,
        CLOSED_CAPTION,
        METADATA
    }

    /**
     * Registers additional customized [com.google.android.exoplayer2.Renderer]s
     * that will be used by the [com.google.android.exoplayer2.source.MediaSource]s to
     * correctly play media.
     *
     * @param type The type for the renderer
     * @param clazz The class of the customized Renderer
     */
    fun registerRenderer(type: RendererType, clazz: Class<out Renderer>) {
        Data.registeredRendererClasses[type]!!.add(clazz.name)
    }

    /**
     * Registers additional [com.google.android.exoplayer2.source.MediaSource]s for the specified file
     * extensions (and regexes). [com.google.android.exoplayer2.source.MediaSource]s registered here will take
     * precedence to the pre-configured ones.
     *
     * @param builder The builder for additional or customized media sources
     */
    fun registerMediaSourceBuilder(builder: MediaSourceProvider.SourceTypeBuilder) {
        Data.sourceTypeBuilders.add(0, builder)
    }

    /**
     * Specifies the provider to use when building [com.google.android.exoplayer2.upstream.DataSource.Factory]
     * instances for use with the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder]s. This will
     * only be used for builders that haven't customized the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder.buildDataSourceFactory]
     * method.
     *
     * @param provider The provider to use for the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder]s
     */
    fun setDataSourceFactoryProvider(provider: DataSourceFactoryProvider?) {
        Data.dataSourceFactoryProvider = provider
    }

    /**
     * Specifies the [LoadControl] to use when building the [com.google.android.exoplayer2.ExoPlayer] instance
     * used in the [com.devbrackets.android.exomedia.ui.widget.VideoView] and [AudioPlayer]. This allows the
     * buffering amounts to be modified to better suit your needs which can be easily specified by using an instance of
     * [com.google.android.exoplayer2.DefaultLoadControl]. When the `loadControl` is `null`
     * the default instance of the [com.google.android.exoplayer2.DefaultLoadControl] will be used. This will only
     * take effect for any instances created *after* this was set.
     *
     * @param loadControl The [LoadControl] to use for any new [com.google.android.exoplayer2.ExoPlayer] instances
     */
    fun setLoadControl(loadControl: LoadControl?) {
        Data.loadControl = loadControl
    }

    object Data {
        val registeredRendererClasses: MutableMap<RendererType, MutableList<String>> = HashMap()
        val sourceTypeBuilders: MutableList<MediaSourceProvider.SourceTypeBuilder> = ArrayList()
        @Volatile
        var dataSourceFactoryProvider: DataSourceFactoryProvider? = null
        @Volatile
        var loadControl: LoadControl? = null
        @Volatile
        var mediaSourceProvider = MediaSourceProvider()

        init {
            instantiateRendererClasses()
            instantiateSourceProviders()
        }

        private fun instantiateRendererClasses() {
            // Instantiates the required values
            registeredRendererClasses[RendererType.AUDIO] = LinkedList()
            registeredRendererClasses[RendererType.VIDEO] = LinkedList()
            registeredRendererClasses[RendererType.CLOSED_CAPTION] = LinkedList()
            registeredRendererClasses[RendererType.METADATA] = LinkedList()

            // Adds the ExoPlayer extension library renderers
            registeredRendererClasses[RendererType.AUDIO]?.apply {
                add("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer")
                add("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer")
                add("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer")
            }

            registeredRendererClasses[RendererType.VIDEO]?.apply {
                add("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer")
            }
        }

        private fun instantiateSourceProviders() {
            // Adds the HLS, SmoothStream, and MPEG Dash registrations
            sourceTypeBuilders.add(MediaSourceProvider.SourceTypeBuilder(HlsMediaSourceBuilder(), null, ".m3u8", ".*\\.m3u8.*"))
            sourceTypeBuilders.add(MediaSourceProvider.SourceTypeBuilder(DashMediaSourceBuilder(), null, ".mpd", ".*\\.mpd.*"))
            sourceTypeBuilders.add(MediaSourceProvider.SourceTypeBuilder(SsMediaSourceBuilder(), null, ".ism", ".*\\.ism.*"))
        }
    }
}
