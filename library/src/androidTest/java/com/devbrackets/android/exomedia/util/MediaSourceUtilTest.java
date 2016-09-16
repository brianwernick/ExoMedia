package com.devbrackets.android.exomedia.util;

import android.net.Uri;

import com.devbrackets.android.exomedia.type.MediaSourceType;

import org.junit.Test;

import static com.devbrackets.android.exomedia.type.MediaSourceType.MP4;
import static com.devbrackets.android.exomedia.type.MediaSourceType.UNKNOWN;
import static org.junit.Assert.assertEquals;

public class MediaSourceUtilTest {

    @Test
    public void shouldRecogniseMp4FromUri() {
        verify(MP4, "http://host.com/file.mp4");
        verify(MP4, "http://host.com/file.mp4?query=\"param\"");
    }

    @Test
    public void shouldReturnUnknownForAnEmptyPath() {
        verify(UNKNOWN, "http://host.com/");
    }

    private void verify(MediaSourceType mediaSourceType, String uriString) {
        assertEquals(mediaSourceType, MediaSourceUtil.getType(Uri.parse(uriString)));
    }

}