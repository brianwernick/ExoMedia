package com.devbrackets.android.exomedia.util;

public enum MediaType {
    SMOOTH_STREAM(".ism", "application/vnd.ms-sstr+xml"),
    DASH(".mpd", "application/dash+xml"),
    HLS(".m3u8", "application/x-mpegurl"),
    MP4(".mp4", "video/mp4"),
    FMP4(".fmp4", "video/fmp4"),
    M4A(".m4a", "video/m4a"),
    MP3(".mp3", "audio/mp3"),
    TS(".ts", "video/mp2t"),
    AAC(".aac", "audio/aac"),
    WEBM(".webm", "video/webm"),
    MKV(".mkv", "video/mkv"),
    UNKNOWN("", "");

    private String extension;
    private String mimeType;

    MediaType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }
}
