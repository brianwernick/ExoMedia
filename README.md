ExoMedia
============
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
    jcenter();
}

dependencies {
    //stable
    compile 'com.devbrackets.android:exomedia:2.5.6'
    //or preview
    compile 'com.devbrackets.android:exomedia:3.0.0-preview1'
}
```

Example
-------
The EMVideoView (EM for ExoMedia) can be added in your layout files like any other Android view.
Note that the latter `ui.widget.EMVideoView` should only be used for versions above `3.0.0`.

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:EMVideoView="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

	<!-- For versions below 3.0.0 -->	
	<com.devbrackets.android.exomedia.EMVideoView
		android:id="@+id/video_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"
		EMVideoView:defaultControlsEnabled="true"/>
		
	<!-- For versions 3.+ -->
	<com.devbrackets.android.exomedia.ui.widget.EMVideoView
		android:id="@+id/video_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"
		EMVideoView:defaultControlsEnabled="true"/>
		
</RelativeLayout>
```

While in your Activity or Fragment you treat it like a standard Android VideoView

```java
private void setupVideoView() {
	EMVideoView emVideoView = (EMVideoView)findViewById(R.id.video_view);
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


Attribution
-----------
* Uses [AppCompat-v7](http://developer.android.com/tools/support-library/features.html#v7-appcompat) licensed under [Apache 2.0][Apache 2.0]
* Uses [ExoPlayer][ExoPlayer] licensed under [Apache 2.0][Apache 2.0]
* Uses [Material Design icons][Design Icons] licensed under [Creative Commons 4.0][CC 4.0]  

 [Apache 2.0]: http://www.apache.org/licenses/LICENSE-2.0
 [CC 4.0]: http://creativecommons.org/licenses/by/4.0/
 [CTS]: https://source.android.com/compatibility/cts/index.html
 [Design Icons]: https://github.com/google/material-design-icons
 [ExoPlayer]: https://github.com/google/ExoPlayer
 [Java Docs]: https://devbrackets.com/dev/libs/docs/exomedia/2.5.0/index.html
 [JCenter]: https://bintray.com/brianwernick/maven/ExoMedia/view#files
 [Website]: https://devbrackets.com/dev/libs/exomedia.html
