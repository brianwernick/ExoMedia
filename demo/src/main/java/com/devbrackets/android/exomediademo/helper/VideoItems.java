package com.devbrackets.android.exomediademo.helper;

import java.util.LinkedList;
import java.util.List;

/**
 * A simple holder for the available video items
 */
public class VideoItems {
    private static final List<VideoItem> items;

    static {
        items = new LinkedList<>();
        items.add(new VideoItem("MP4 - Popeye", "https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"));
        items.add(new VideoItem("WEBM - Big Buck Bunny", "http://video.webmfiles.org/big-buck-bunny_trailer.webm"));
        items.add(new VideoItem("DASH - ?", "http://bitlivedemo-a.akamaihd.net/mpds/stream.php?streamkey=bitcodin")); //TODO find a different one
        items.add(new VideoItem("HLS - ?", "http://playertest.longtailvideo.com/adaptive/wowzaid3/playlist.m3u8"));
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
