package com.devbrackets.android.exomedia.core;

import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;

/**
 * Created by Guillermo Raya on 20/06/2016.
 */
public abstract class EMListenerMuxNotifier {
  public void onSeekComplete() {
    //Purposefully left blank
  }

  public void onBufferUpdated(int percent) {
    //Purposefully left blank
  }

  public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees, float pixelWidthHeightRatio) {
    //Purposefully left blank
  }

  public void onPrepared() {
    //Purposefully left blank
  }

  public void onPreviewImageStateChanged(boolean toVisible) {
    //Purposefully left blank
  }

  public abstract boolean shouldNotifyCompletion(long endLeeway);

  public abstract void onExoPlayerError(EMExoPlayer emExoPlayer, Exception e);

  public abstract void onMediaPlaybackEnded();

  public abstract void onSeekCompletion();
}
