package com.devbrackets.android.exomediademo.helper;

import java.util.LinkedList;
import java.util.List;

/**
 * A simple holder for the available audio items.  These items are
 * CreativeCommons licensed or other Public Domain licensed audio files
 * from LibriVox.  Additional files can be found at
 * https://archive.org/details/count_monte_cristo_0711_librivox
 */
public class AudioItems {
    private static final List<AudioItem> items;

    static {
        String imageUrl = "https://ia902708.us.archive.org/3/items/count_monte_cristo_0711_librivox/Count_Monte_Cristo_1110.jpg?cnt=0";

        items = new LinkedList<>();
        items.add(new AudioItem("Marseilles -- The Arrival", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_001_dumas.mp3", imageUrl));
        items.add(new AudioItem("Father and Son", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_002_dumas.mp3", imageUrl));
        items.add(new AudioItem("The Catalans", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_003_dumas.mp3", imageUrl));
        items.add(new AudioItem("Conspiracy", "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_004_dumas.mp3", imageUrl));
    }

    public static List<AudioItem> getItems() {
        return items;
    }

    public static class AudioItem {
        String title;
        String mediaUrl;
        String artworkUrl;

        public AudioItem(String title, String mediaUrl, String artworkUrl) {
            this.title = title;
            this.mediaUrl = mediaUrl;
            this.artworkUrl = artworkUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public String getArtworkUrl() {
            return artworkUrl;
        }
    }
}
