ExoMedia
============
A Utility class that wraps the ExoPlayer in to a standardized
View and API much like the built in Android VideoView and MediaPlayer.

Since the ExoPlayer is only supported on JellyBean or greater devices that
pass the Android Compatibility Test Suite (CTS), the EMVideoView will gracefully
fall back to using the Android VideoView.  Similarly the EMAudioPlayer will fall
back to the MediaPlayer.

This is useful for supporting Http Live Streaming (HLS) and full HD (1080p +) playback
since the Android VideoView struggles with.

License
-------

    Copyright 2015 Brian Wernick

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