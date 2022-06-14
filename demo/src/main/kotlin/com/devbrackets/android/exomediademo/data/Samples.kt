package com.devbrackets.android.exomediademo.data

object Samples {
    private const val audioImage =
        "https://ia902708.us.archive.org/3/items/count_monte_cristo_0711_librivox/Count_Monte_Cristo_1110.jpg?cnt=0"

    val audio by lazy {
        listOf(
            Sample(
                "Marseilles -- The Arrival",
                "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_001_dumas.mp3",
                Sample.Category.AUDIO,
                audioImage
            ),
            Sample(
                "Father and Son",
                "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_002_dumas.mp3",
                Sample.Category.AUDIO,
                audioImage
            ),
            Sample(
                "The Catalans",
                "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_003_dumas.mp3",
                Sample.Category.AUDIO,
                audioImage
            ),
            Sample(
                "Conspiracy",
                "https://archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_004_dumas.mp3",
                Sample.Category.AUDIO,
                audioImage
            )
        )
    }

    val video by lazy {
        listOf(
            Sample(
                "FLV - Big Buck Bunny by Blender",
                "http://vod.leasewebcdn.com/bbb.flv?ri=1024&rs=150&start=0",
                Sample.Category.VIDEO
            ),
            Sample(
                "HLS - ArtBeats",
                "http://cdn-fms.rbs.com.br/vod/hls_sample1_manifest.m3u8",
                Sample.Category.VIDEO
            ),
            Sample(
                "HLS - Sintel by Blender",
                "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                Sample.Category.VIDEO
            ),
            Sample(
                "MKV - Android Screens",
                "http://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv",
                Sample.Category.VIDEO
            ),
            Sample(
                "MP4 (VP9) - Google Glass",
                "http://demos.webmproject.org/exoplayer/glass.mp4",
                Sample.Category.VIDEO
            ),
            Sample(
                "MPEG DASH - Sintel by Blender",
                "https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd",
                Sample.Category.VIDEO
            ),
            Sample(
                "MPEG DASH - Big Buck Bunny by Blender, Live",
                "https://wowzaec2demo.streamlock.net/live/bigbuckbunny/manifest_mpm4sav_mvtime.mpd",
                Sample.Category.VIDEO
            ),
            Sample(
                "Smooth Stream - Caminandes: Llama Drama by Blender",
                "http://amssamples.streaming.mediaservices.windows.net/634cd01c-6822-4630-8444-8dd6279f94c6/CaminandesLlamaDrama4K.ism/manifest",
                Sample.Category.VIDEO
            ),
            Sample(
                "Smooth Stream - Tears of Steel Teaser by Blender",
                "http://amssamples.streaming.mediaservices.windows.net/3d7eaff9-39fa-442f-81cc-f2ea7db1797e/TearsOfSteelTeaser.ism/manifest",
                Sample.Category.VIDEO
            ),
            Sample(
                "WEBM - Big Buck Bunny",
                "http://dl1.webmfiles.org/big-buck-bunny_trailer.webm",
                Sample.Category.VIDEO
            ),
            Sample(
                "WEBM - Elephants Dream",
                "http://dl1.webmfiles.org/elephants-dream.webm",
                Sample.Category.VIDEO
            )
        )
    }

    class Sample(
        val title: String,
        val mediaUrl: String,
        val category: Category,
        val artworkUrl: String? = null
    ) {
        enum class Category {
            AUDIO,
            VIDEO
        }
    }
}
