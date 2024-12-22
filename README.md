![Maven Central](https://img.shields.io/maven-central/v/com.devbrackets.android/exomedia)

ExoMedia
============
ExoMedia is an audio/video playback library for Android built on top of the ExoPlayer
with straightforward APIs and integrations. This library focuses on quick setup, handling 
common audio and video playback needs while also providing extensibility for more custom
use cases.

ExoMedia vs ExoPlayer
------
The [ExoPlayer][ExoPlayer] is an advanced media player for Android that is highly customizable,
however that comes at the cost of a more complex setup and configuration process. This customizability
is great when it's needed however can be daunting when you need to play a simple audio or video file.

ExoMedia is a more high-level abstraction of media playback that abstracts some of the customizability 
provided by the [ExoPlayer][ExoPlayer] into simple functions and callbacks, keeping the required 
configuration to a minimum. 


Use
-------
The latest version can be found at [Maven Central][MavenCentral].

```gradle
repositories {
  mavenCentral()
}

dependencies {
  implementation 'com.devbrackets.android:exomedia:5.2.0'
}
```


Quick Start
-------
The ExoMedia VideoView can be added in your layout files like any other Android view.

```xml
<RelativeLayout 
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:layout_width="match_parent"
  android:layout_height="match_parent">

	<com.devbrackets.android.exomedia.ui.widget.VideoView
		android:id="@+id/video_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent" />
</RelativeLayout>
```

While in your Activity or Fragment you treat it like a standard Android VideoView

```kotlin
private lateinit var videoView: VideoView

private fun setupVideoView() {
  // Make sure to use the correct VideoView import
  videoView = findViewById(R.id.video_view) as VideoView
  videoView.setOnPreparedListener(this)

  // For now we just picked an arbitrary item to play
  videoView.setMedia(Uri.parse("https://www.devbrackets.com/media/samples/video/big_buck_bunny.mp4"))
}

@Override
fun onPrepared() {
  //Starts the video playback as soon as it is ready
  videoView.start()
}
```


License
-------
    Copyright 2015-2024 ExoMedia Contributors

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
* Uses [Kotlin](https://kotlinlang.org/) licensed under [Apache 2.0][Apache 2.0] 
* Uses [AndroidX Media3](https://developer.android.com/jetpack/androidx/releases/media3) licensed under [Apache 2.0][Apache 2.0]
* Uses [ConstraintLayout](https://developer.android.com/training/constraint-layout) licensed under [Apache 2.0][Apache 2.0]
* Uses [AndroidX AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat) licensed under [Apache 2.0][Apache 2.0]
* Uses [Material Design icons][Design Icons] licensed under [Apache 2.0][Apache 2.0]

 [Apache 2.0]: http://www.apache.org/licenses/LICENSE-2.0
 [CTS]: https://source.android.com/compatibility/cts/index.html
 [Design Icons]: https://github.com/google/material-design-icons
 [ExoPlayer]: https://github.com/androidx/media
 [MavenCentral]: https://search.maven.org/artifact/com.devbrackets.android/exomedia