ExoMedia
============
[//]: # (SEO terms: Simple ExoPlayer, ExoPlayer TextureView, ExoPlayer SurfaceView, ExoPlayer VideoView, ExoPlayer Audio)
ExoMedia is a media playback library with similar APIs to the Android MediaPlayer
and VideoView that uses the [ExoPlayer][ExoPlayer] as a backing when possible, 
otherwise the default Android MediaPlayer and VideoView are used.

The [ExoPlayer][ExoPlayer] is only supported on devices that pass the [compatibility Test Suite][CTS]
and that are JellyBean (API 16) or greater.  The [ExoPlayer][ExoPlayer] provides 
additional support for streaming (HLS, DASH, etc.) and full HD (1080p +) 

Website And Documentation
--------
The ExoMedia website can be found [here][Website]  
The ExoMedia documentation website can be found on the website linked above or [here][Java Docs]

Use
-------
The latest AAR (Android Archive) files can be downloaded from [JCenter][JCenter]  
Or included in your gradle dependencies

```gradle
repositories {
    jcenter()
}

dependencies {
    implementation 'com.devbrackets.android:exomedia:4.1.0'
}
```

Example
-------
The ExoMedia VideoView can be added in your layout files like any other Android view.

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

	<com.devbrackets.android.exomedia.ui.widget.VideoView
		android:id="@+id/video_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"
		app:useDefaultControls="true"/>
		
</RelativeLayout>
```

While in your Activity or Fragment you treat it like a standard Android VideoView

```java
private VideoView videoView;

private void setupVideoView() {
	// Make sure to use the correct VideoView import
	videoView = (VideoView)findViewById(R.id.video_view);
	videoView.setOnPreparedListener(this);

    //For now we just picked an arbitrary item to play
    videoView.setVideoURI(Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"));
}

@Override
public void onPrepared() {
	//Starts the video playback as soon as it is ready
	videoView.start();
}
```


License
-------
    Copyright 2015-2017 Brian Wernick

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


Attribution
-----------
* Uses [AppCompat-v7](http://developer.android.com/tools/support-library/features.html#v7-appcompat) licensed under [Apache 2.0][Apache 2.0]
* Uses [ExoPlayer][ExoPlayer] licensed under [Apache 2.0][Apache 2.0]
* Uses [Material Design icons][Design Icons] licensed under [Apache 2.0][Apache 2.0]

 [Apache 2.0]: http://www.apache.org/licenses/LICENSE-2.0
 [CTS]: https://source.android.com/compatibility/cts/index.html
 [Design Icons]: https://github.com/google/material-design-icons
 [ExoPlayer]: https://github.com/google/ExoPlayer
 [Java Docs]: https://devbrackets.com/dev/libs/docs/exomedia/4.0.0/index.html
 [JCenter]: https://bintray.com/brianwernick/maven/ExoMedia/view#files
 [Website]: https://devbrackets.com/dev/libs/exomedia.html
