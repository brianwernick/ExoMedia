<code>
<style>
    .website-link {
        color: white;
        padding: 0.5em 1em;
        margin-right: 1em;
        text-decoration: none;
        
        background: #63cc39; /* For browsers that do not support gradients */
        background: -webkit-linear-gradient(#a0f74a, #63cc39); /* For Safari 5.1 to 6.0 */
        background: -o-linear-gradient(#a0f74a, #63cc39); /* For Opera 11.1 to 12.0 */
        background: -moz-linear-gradient(#a0f74a, #63cc39); /* For Firefox 3.6 to 15 */
        background: linear-gradient(#a0f74a, #63cc39); /* Standard syntax */
    }
    
    .javadoc-link {
        color: white;
        padding: 0.5em 1em;
        text-decoration: none;
        
        background: #1b73d1; /* For browsers that do not support gradients */
        background: -webkit-linear-gradient(#78bdfa, #1b73d1); /* For Safari 5.1 to 6.0 */
        background: -o-linear-gradient(#78bdfa, #1b73d1); /* For Opera 11.1 to 12.0 */
        background: -moz-linear-gradient(#78bdfa, #1b73d1); /* For Firefox 3.6 to 15 */
        background: linear-gradient(#78bdfa, #1b73d1); /* Standard syntax */
    }
</style>


<div style="padding-top:0.5em;" align="right">
    <a class="website-link" href="http://www.devbrackets.com/dev/libs/exomedia.html">
        Website
    </a>
    <a class="javadoc-link" href="http://www.devbrackets.com/dev/libs/docs/exomedia/3.0.0/index.html">
        Java Doc
    </a>
</div>
</code>


ExoMedia
============
ExoMedia is a media playback library with similar APIs to the Android MediaPlayer
and VideoView that uses the [ExoPlayer][ExoPlayer] as a backing when possible, 
otherwise the default Android MediaPlayer and VideoView are used.

The [ExoPlayer][ExoPlayer] is only supported on devices that pass the [compatibility Test Suite][CTS]
and that are JellyBean (API 16) or greater.  The [ExoPlayer][ExoPlayer] provides 
additional support for streaming (HLS, DASH, etc.) and full HD (1080p +) 

Use
-------
The latest AAR (Android Archive) files can be downloaded from [JCenter][JCenter]  
Or included in your gradle dependencies

```gradle
repositories {
    jcenter();
}

dependencies {
    //...
    compile 'com.devbrackets.android:exomedia:3.0.0'
}
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
Uses [Material Design icons][Design Icons] icons by Google licensed under [Creative Commons 4.0][CC 4.0]  

Uses [ExoPlayer][ExoPlayer] by Google

    Copyright 2016 Google Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [Design Icons]: https://github.com/google/material-design-icons
 [CC 4.0]: http://creativecommons.org/licenses/by/4.0/
 [ExoPlayer]:https://github.com/google/ExoPlayer
 [CTS]:https://source.android.com/compatibility/cts/index.html
 [JCenter]: https://bintray.com/brianwernick/maven/ExoMedia/view#files
 [Website]: http://devbrackets.com/dev/libs/exomedia.html
 [Java Docs]: http://devbrackets.com/dev/libs/docs/exomedia/3.0.0/index.html
