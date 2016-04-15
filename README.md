ExoMedia
============
A Utility class that wraps the ExoPlayer in to a standardized
View and API much like the built in Android VideoView and MediaPlayer.
Additionally, to simplify playback of media lists a playlist manager
and playlist service have been provided.

Since the ExoPlayer is only supported on JellyBean or greater devices that
pass the Android Compatibility Test Suite (CTS), the EMVideoView will gracefully
fall back to using the Android VideoView.  Similarly the EMAudioPlayer will fall
back to the MediaPlayer.

This is useful for supporting Http Live Streaming (HLS) and full HD (1080p +) playback
since the Android VideoView struggles with those.

**NOTE:** HLS, DASH, and other streaming protocols are not supported on Android 4.0.* and below
due to constraints with the Android MediaPlayer and VideoView


Website And Documentation
-------
The ExoMedia website can be found [here][4]

The ExoMedia documentation website can be found on the website linked above or [here][5]


Use
-------
The latest AAR (Android Archive) files can be downloaded from JCenter [ExoMedia][3]

Or included in your gradle dependencies

```gradle
//stable
compile 'com.devbrackets.android:exomedia:2.5.6'
//or preview
compile 'com.devbrackets.android:exomedia:3.0.0-preview1'
```

Example
-------
The EMVideoView (EM for ExoMedia) can be added in your layout files like any other Android view.

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:EMVideoView="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

    <com.devbrackets.android.exomedia.EMVideoView
        android:id="@+id/video_play_activity_video_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        EMVideoView:defaultControlsEnabled="true"/>
</RelativeLayout>

```

While in your Activity or Fragment you treat it like a standard Android VideoView

```java
private void setupVideoView() {
	EMVideoView emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
	emVideoView.setOnPreparedListener(this);

    //For now we just picked an arbitrary item to play.  More can be found at
    //https://archive.org/details/more_animation
    emVideoView.setVideoURI(Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"));
}

@Override
public void onPrepared(MediaPlayer mp) {
	//Starts the video playback as soon as it is ready
	emVideoView.start();
}
```


License
-------

    Copyright 2016 Brian Wernick

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


Additionally, the media playback icons are from the google Material
design icons without any changes.  The full set of icons can be found
at [https://github.com/google/material-design-icons][1] which are licensed
under [Attribution 4.0 International][2]



 [1]: https://github.com/google/material-design-icons
 [2]: http://creativecommons.org/licenses/by/4.0/
 [3]: https://bintray.com/brianwernick/maven/ExoMedia/view#files
 [4]: http://devbrackets.com/dev/libs/exomedia.html
 [5]: http://devbrackets.com/dev/libs/docs/exomedia/2.5.0/index.html
