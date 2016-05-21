package com.devbrackets.android.exomediademo.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Samples {
    @NonNull
    private static final List<Sample> audioSamples;
    @NonNull
    private static final List<Sample> videoSamples;

    static {
        String audioImage = "https://ia902708.us.archive.org/3/items/count_monte_cristo_0711_librivox/Count_Monte_Cristo_1110.jpg?cnt=0";

        //Audio items
        audioSamples = new LinkedList<>();
        audioSamples.add(new Sample("Marseilles -- The Arrival", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_001_dumas.mp3", audioImage));
        audioSamples.add(new Sample("Father and Son", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_002_dumas.mp3", audioImage));
        audioSamples.add(new Sample("The Catalans", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_003_dumas.mp3", audioImage));
        audioSamples.add(new Sample("Conspiracy", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_004_dumas.mp3", audioImage));


        //Video items
        videoSamples = new ArrayList<>();
        videoSamples.add(new Sample("3GP - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/3gp/240/big_buck_bunny_240p_10mb.3gp"));
//        videoSamples.add(new Sample("FLV - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/flv/720/big_buck_bunny_720p_5mb.flv"));
        videoSamples.add(new Sample("HLS - Wildlife", "http://playertest.longtailvideo.com/adaptive/wowzaid3/playlist.m3u8"));
        videoSamples.add(new Sample("MKV - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/mkv/480/big_buck_bunny_480p_10mb.mkv"));
        videoSamples.add(new Sample("MP4 - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/mp4/480/big_buck_bunny_480p_10mb.mp4"));
        videoSamples.add(new Sample("MPEG DASH - Sintel by Blender", "https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd"));
        videoSamples.add(new Sample("MPEG DASH - Big Buck Bunny by Blender, Live", "https://wowzaec2demo.streamlock.net/live/bigbuckbunny/manifest_mpm4sav_mvtime.mpd"));
        videoSamples.add(new Sample("Smooth Stream - Caminandes: Llama Drama by Blender", "http://amssamples.streaming.mediaservices.windows.net/634cd01c-6822-4630-8444-8dd6279f94c6/CaminandesLlamaDrama4K.ism/manifest"));
        videoSamples.add(new Sample("Smooth Stream - Tears of Steel Teaser by Blender", "http://amssamples.streaming.mediaservices.windows.net/3d7eaff9-39fa-442f-81cc-f2ea7db1797e/TearsOfSteelTeaser.ism/manifest"));
        videoSamples.add(new Sample("WEBM - Big Buck Bunny", "http://video.webmfiles.org/big-buck-bunny_trailer.webm"));
    }

    @NonNull
    public static List<Sample> getAudioSamples() {
        return audioSamples;
    }

    @NonNull
    public static List<Sample> getVideoSamples() {
        return videoSamples;
    }

    /**
     * A container for the information associated with a
     * sample media item.
     */
    public static class Sample {
        @NonNull
        private String title;
        @NonNull
        private String mediaUrl;
        @Nullable
        private String artworkUrl;

        public Sample(@NonNull String title, @NonNull String mediaUrl) {
            this(title, mediaUrl, null);
        }

        public Sample(@NonNull String title, @NonNull String mediaUrl, @Nullable String artworkUrl) {
            this.title = title;
            this.mediaUrl = mediaUrl;
            this.artworkUrl = artworkUrl;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        @NonNull
        public String getMediaUrl() {
            return mediaUrl;
        }

        @Nullable
        public String getArtworkUrl() {
            return artworkUrl;
        }
    }
}
