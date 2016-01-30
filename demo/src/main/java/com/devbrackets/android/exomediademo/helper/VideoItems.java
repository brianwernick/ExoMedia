package com.devbrackets.android.exomediademo.helper;

import java.util.ArrayList;
import java.util.List;

public class VideoItems {
    private final static List<VideoItem> items;

    static {
        items = new ArrayList<>();

        items.add(new VideoItem("3GP - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/3gp/240/big_buck_bunny_240p_10mb.3gp"));
//        items.add(new VideoItem("FLV - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/flv/480/big_buck_bunny_480p_10mb.flv"));
        items.add(new VideoItem("HLS - Wildlife", "http://playertest.longtailvideo.com/adaptive/wowzaid3/playlist.m3u8"));
        items.add(new VideoItem("MKV - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/mkv/480/big_buck_bunny_480p_10mb.mkv"));
        items.add(new VideoItem("MP4 - Big Buck Bunny by Blender", "http://www.sample-videos.com/video/mp4/480/big_buck_bunny_480p_10mb.mp4"));
        items.add(new VideoItem("MPEG DASH - Sintel by Blender", "https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd"));
        items.add(new VideoItem("MPEG DASH - Big Buck Bunny by Blender, Live", "https://wowzaec2demo.streamlock.net/live/bigbuckbunny/manifest_mpm4sav_mvtime.mpd"));
        items.add(new VideoItem("Smooth Stream - Caminandes: Llama Drama by Blender", "http://amssamples.streaming.mediaservices.windows.net/634cd01c-6822-4630-8444-8dd6279f94c6/CaminandesLlamaDrama4K.ism/manifest"));
        items.add(new VideoItem("Smooth Stream - Tears of Steel Teaser by Blender", "http://amssamples.streaming.mediaservices.windows.net/3d7eaff9-39fa-442f-81cc-f2ea7db1797e/TearsOfSteelTeaser.ism/manifest"));
        items.add(new VideoItem("WEBM - Big Buck Bunny", "http://video.webmfiles.org/big-buck-bunny_trailer.webm"));
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
