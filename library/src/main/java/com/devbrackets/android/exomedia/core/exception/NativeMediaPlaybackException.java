package com.devbrackets.android.exomedia.core.exception;

import android.media.MediaPlayer;

/**
 * An Exception to indicate that the Native {@link MediaPlayer} has
 * had an error during or while attempting playback
 */
public class NativeMediaPlaybackException extends Exception {
    public final int what;
    public final int extra;

    public NativeMediaPlaybackException(int what, int extra) {
        super(MediaPlayer.class.getName() + " has had the error " + what + " with extras " + extra);

        this.what = what;
        this.extra = extra;
    }
}
