package com.devbrackets.android.exomedia.core.renderer;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.ExoMedia;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataDecoderFactory;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides all the necessary {@link com.google.android.exoplayer2.Renderer}s
 */
@SuppressWarnings("WeakerAccess")
public class RendererProvider {
    @NonNull
    protected Context context;
    @NonNull
    protected Handler handler;

    @NonNull
    protected TextOutput captionListener;
    @NonNull
    protected MetadataOutput metadataListener;
    @NonNull
    protected AudioRendererEventListener audioRendererEventListener;
    @NonNull
    protected VideoRendererEventListener videoRendererEventListener;

    @Nullable
    protected DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    protected int droppedFrameNotificationAmount = 50;
    protected int videoJoiningTimeMs = 5_000;

    public RendererProvider(@NonNull Context context, @NonNull Handler handler, @NonNull TextOutput captionListener, @NonNull MetadataOutput metadataListener,
                            @NonNull AudioRendererEventListener audioRendererEventListener, @NonNull VideoRendererEventListener videoRendererEventListener) {
        this.context = context;
        this.handler = handler;
        this.captionListener = captionListener;
        this.metadataListener = metadataListener;
        this.audioRendererEventListener = audioRendererEventListener;
        this.videoRendererEventListener = videoRendererEventListener;
    }

    public void setDrmSessionManager(@Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        this.drmSessionManager = drmSessionManager;
    }

    public void setDroppedFrameNotificationAmount(int droppedFrameNotificationAmount) {
        this.droppedFrameNotificationAmount = droppedFrameNotificationAmount;
    }

    public void setVideoJoiningTimeMs(int videoJoiningTimeMs) {
        this.videoJoiningTimeMs = videoJoiningTimeMs;
    }

    @NonNull
    public List<Renderer> generate() {
        List<Renderer> renderers = new ArrayList<>();

        renderers.addAll(buildAudioRenderers());
        renderers.addAll(buildVideoRenderers());
        renderers.addAll(buildCaptionRenderers());
        renderers.addAll(buildMetadataRenderers());

        return renderers;
    }

    @NonNull
    protected List<Renderer> buildAudioRenderers() {
        List<Renderer> renderers = new ArrayList<>();

        renderers.add(new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT, drmSessionManager, true, handler, audioRendererEventListener, AudioCapabilities.getCapabilities(context)));

        // Adds any registered classes
        List<String> classNames = ExoMedia.Data.registeredRendererClasses.get(ExoMedia.RendererType.AUDIO);
        if (classNames != null) {
            for (String className: classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(Handler.class, AudioRendererEventListener.class);
                    Renderer renderer = (Renderer) constructor.newInstance(handler, audioRendererEventListener);
                    renderers.add(renderer);
                } catch (Exception e) {
                    // Purposefully left blank
                }
            }
        }

        return renderers;
    }

    @NonNull
    protected List<Renderer> buildVideoRenderers() {
        List<Renderer> renderers = new ArrayList<>();

        renderers.add(new MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT, videoJoiningTimeMs, drmSessionManager, false, handler, videoRendererEventListener, droppedFrameNotificationAmount));

        // Adds any registered classes
        List<String> classNames = ExoMedia.Data.registeredRendererClasses.get(ExoMedia.RendererType.VIDEO);
        if (classNames != null) {
            for (String className: classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(boolean.class, long.class, Handler.class, VideoRendererEventListener.class, int.class);
                    Renderer renderer = (Renderer) constructor.newInstance(true, videoJoiningTimeMs, handler, videoRendererEventListener, droppedFrameNotificationAmount);
                    renderers.add(renderer);
                } catch (Exception e) {
                    // Purposefully left blank
                }
            }
        }

        return renderers;
    }

    @NonNull
    protected List<Renderer> buildCaptionRenderers() {
        List<Renderer> renderers = new ArrayList<>();

        renderers.add(new TextRenderer(captionListener, handler.getLooper()));

        return renderers;
    }

    @NonNull
    protected List<Renderer> buildMetadataRenderers() {
        List<Renderer> renderers = new ArrayList<>();

        renderers.add(new MetadataRenderer(metadataListener, handler.getLooper(), MetadataDecoderFactory.DEFAULT));

        return renderers;
    }
}
