package com.devbrackets.android.exomediademo.helper;

import java.util.ArrayList;
import java.util.List;

public class VideoItems {
    private final static List<VideoItem> items;

    static {
        items = new ArrayList<>();

        items.add(new VideoItem("Big Buck Bunny by Blender (MP4)", "http://www.sample-videos.com/video/mp4/480/big_buck_bunny_480p_10mb.mp4"));
        items.add(new VideoItem("Big Buck Bunny by Blender (FLV)", "http://www.sample-videos.com/video/flv/480/big_buck_bunny_480p_10mb.flv"));
        items.add(new VideoItem("Big Buck Bunny by Blender (MKV)", "http://www.sample-videos.com/video/mkv/480/big_buck_bunny_480p_10mb.mkv"));
        items.add(new VideoItem("Big Buck Bunny by Blender (3GP)", "http://www.sample-videos.com/video/3gp/240/big_buck_bunny_240p_10mb.3gp"));
        items.add(new VideoItem("Caminandes: Llama Drama by Blender (Smooth Stream)", "http://amssamples.streaming.mediaservices.windows.net/634cd01c-6822-4630-8444-8dd6279f94c6/CaminandesLlamaDrama4K.ism/manifest"));
        items.add(new VideoItem("Tears of Steel Teaser by Blender (Smooth Stream)", "http://amssamples.streaming.mediaservices.windows.net/3d7eaff9-39fa-442f-81cc-f2ea7db1797e/TearsOfSteelTeaser.ism/manifest"));
        items.add(new VideoItem("Sintel by Blender (DASH)", "https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd"));
        items.add(new VideoItem("Big Buck Bunny by Blender, Live (DASH)", "https://wowzaec2demo.streamlock.net/live/bigbuckbunny/manifest_mpm4sav_mvtime.mpd"));
        items.add(new VideoItem("Apple Bipbop (HLS)", "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"));
        items.add(new VideoItem("Apple Bipbop 16:9 (HLS)", "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"));

    }

    public static List<VideoItem> getItems() {
        return items;
    }

    public static class VideoItem {
        String title;
        String mediaUrl;

        public VideoItem(String title, String mediaUrl) {
            this.title = title;
            this.mediaUrl = mediaUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

    }
}
