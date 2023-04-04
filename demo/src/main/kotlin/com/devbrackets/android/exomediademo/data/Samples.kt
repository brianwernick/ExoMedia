package com.devbrackets.android.exomediademo.data

object Samples {
  val audio by lazy {
    listOf(
      audioSample("Marseilles -- The Arrival", 1),
      audioSample("Father and Son", 2),
      audioSample("The Catalans", 3),
      audioSample("Conspiracy", 4),
      audioSample("The Marriage Feast", 5),
      audioSample("The Deputy Procureur Du Roi", 6),
      audioSample("The Examination", 7),
      audioSample("The Chateau D'lf", 8),
      audioSample("The Evening of the Betrothal", 9),
      audioSample("The Kings Closet at the Tuileries", 10)
    )
  }

  val video by lazy {
    listOf(
      videoSample("Big Buck Bunny (MP4)", "https://www.devbrackets.com/media/samples/video/big_buck_bunny.mp4"),
      videoSample("Caminandes (FLV)", "https://www.devbrackets.com/media/samples/video/caminandes_01.flv"),
      videoSample("Caminandes (Smooth Stream)", "http://amssamples.streaming.mediaservices.windows.net/634cd01c-6822-4630-8444-8dd6279f94c6/CaminandesLlamaDrama4K.ism/manifest"),
      videoSample("Caminandes 2 (MP4)", "https://www.devbrackets.com/media/samples/video/caminandes_02.mp4"),
      videoSample("Caminandes 3 (MP4)", "https://www.devbrackets.com/media/samples/video/caminandes_03.mp4"),
      videoSample("Coffee Run (MKV)", "https://www.devbrackets.com/media/samples/video/coffee_run.mkv"),
      videoSample("Coffee Run (WebM)", "https://www.devbrackets.com/media/samples/video/coffee_run.webm"),
      videoSample("Elephants Dream (MP4)", "https://www.devbrackets.com/media/samples/video/elephants_dream.mp4"),
      videoSample("Elephants Dream (HLS)", "https://www.devbrackets.com/media/samples/video/elephants_dream/elephants_dream.m3u8"),
      videoSample("Sintel (WebM)", "https://www.devbrackets.com/media/samples/video/sintel.webm"),
      videoSample("Sintel (MPEG Dash)", "https://bitdash-a.akamaihd.net/content/sintel/sintel.mpd"),
      videoSample("Tears of Steel (MP4)", "https://www.devbrackets.com/media/samples/video/tears_of_steel.mp4"),
      videoSample("Tears of Steel - Live (HLS)", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
    )
  }

  private fun audioSample(title: String, chapter: Int): Sample {
    val url = "https://www.devbrackets.com/media/samples/audio/count_of_monte_cristo/librivox_%03d.mp3".format(chapter)
    val image = "https://www.devbrackets.com/media/samples/audio/count_of_monte_cristo/librivox_cover.jpg"

    return Sample(title, url, Sample.Category.AUDIO, image)
  }

  private fun videoSample(title: String, url: String): Sample {
    return Sample(title, url, Sample.Category.VIDEO)
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
