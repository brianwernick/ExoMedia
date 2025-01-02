package com.devbrackets.android.exomediademo.ui.media.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.devbrackets.android.exomedia.util.millisToFormattedDuration
import com.devbrackets.android.exomediademo.ui.support.compose.ScreenScaffold
import com.devbrackets.android.exomediademo.ui.support.compose.forwardingPainter
import com.devbrackets.android.exomediademo.ui.support.compose.seek.SeekState
import com.devbrackets.android.exomediademo.ui.support.compose.seek.Seekbar
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme
import com.devbrackets.android.playlistcore.data.PlaybackState

@Composable
internal fun AudioPlayerScreen(
  viewModel: AudioPlayerViewModel,
  onBackClicked: () -> Unit,
) {
  val playbackState = viewModel.playbackState.collectAsState(null)
  val playbackItem = viewModel.playbackItem.collectAsState(null)
  val playbackPosition = viewModel.playbackPosition.collectAsState(null)
  val playbackDuration = viewModel.playbackDuration.collectAsState(null)

  AudioPlayerScreen(
    playbackState = playbackState,
    playbackItem = playbackItem,
    playbackPosition = playbackPosition,
    playbackDuration = playbackDuration,
    onBackClicked = onBackClicked,
    onPlayPause = viewModel::playPause,
    onSeekPrevious = viewModel::seekPrevious,
    onSeekNext = viewModel::seekNext,
    onSeek = viewModel::seek
  )
}

@Composable
private fun AudioPlayerScreen(
  playbackState: State<PlaybackState?>,
  playbackItem: State<AudioPlayerViewModel.PlaybackItem?>,
  playbackPosition: State<Long?>,
  playbackDuration: State<Long?>,
  onBackClicked: () -> Unit,
  onPlayPause: () -> Unit,
  onSeekPrevious: () -> Unit,
  onSeekNext: () -> Unit,
  onSeek: (SeekState) -> Unit,
) {
  val seekPosition = remember { mutableStateOf<Long?>(null) }

  ScreenScaffold(
    title = playbackItem.value?.title.orEmpty(),
    subTitle = playbackItem.value?.album,
    onBackClick = onBackClicked
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .playerBackground(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceEvenly
    ) {
      Artwork(
        playbackItem = playbackItem,
        modifier = Modifier
          .padding(horizontal = 56.dp)
          .size(256.dp)
          .fillMaxSize()
          .aspectRatio(1f)
      )

      PlaybackControls(
        playbackState = playbackState,
        playbackItem = playbackItem,
        playbackPosition = playbackPosition,
        playbackDuration = playbackDuration,
        seekPosition = seekPosition,
        onPlayPause = onPlayPause,
        onSeekPrevious = onSeekPrevious,
        onSeekNext = onSeekNext,
        onSeek = onSeek,
        modifier = Modifier
          .padding(horizontal = 24.dp)
          .widthIn(max = 540.dp)
          .fillMaxWidth()
      )
    }
  }
}

@Composable
private fun PlaybackControls(
  playbackState: State<PlaybackState?>,
  playbackItem: State<AudioPlayerViewModel.PlaybackItem?>,
  playbackPosition: State<Long?>,
  playbackDuration: State<Long?>,
  seekPosition: MutableState<Long?>,
  onPlayPause: () -> Unit,
  onSeekPrevious: () -> Unit,
  onSeekNext: () -> Unit,
  onSeek: (SeekState) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    PlaybackPosition(
      playbackPosition = playbackPosition,
      playbackDuration = playbackDuration,
      seekPosition = seekPosition,
      modifier = Modifier.align(Alignment.End)
    )

    Seekbar(
      position = playbackPosition,
      maxValue = playbackDuration,
      enabled = when (playbackState.value) {
        PlaybackState.PLAYING, PlaybackState.PAUSED, PlaybackState.SEEKING -> true
        else -> false
      },
      onSeek = {
        onSeek(it)
        when (it) {
          is SeekState.Started -> seekPosition.value = it.position
          is SeekState.Seeking -> seekPosition.value = it.position
          is SeekState.Finished -> seekPosition.value = null
        }
      },
      modifier = Modifier.fillMaxWidth(),
    )

    PlaybackActions(
      playbackState = playbackState,
      playbackItem = playbackItem,
      onPlayPause = onPlayPause,
      onSeekPrevious = onSeekPrevious,
      onSeekNext = onSeekNext,
      modifier = Modifier
        .padding(top = 24.dp)
        .fillMaxWidth()
    )
  }
}

@Composable
private fun PlaybackActions(
  playbackState: State<PlaybackState?>,
  playbackItem: State<AudioPlayerViewModel.PlaybackItem?>,
  onPlayPause: () -> Unit,
  onSeekPrevious: () -> Unit,
  onSeekNext: () -> Unit,
  modifier: Modifier = Modifier
) {
  val playPauseVector = remember(playbackState.value) {
    when (playbackState.value) {
      PlaybackState.PLAYING -> Icons.Rounded.Pause
      else -> Icons.Rounded.PlayArrow
    }
  }

  val playPauseEnabled = remember(playbackState.value) {
    when (playbackState.value) {
      PlaybackState.PLAYING, PlaybackState.PAUSED -> true
      else -> false
    }
  }

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterHorizontally)
  ) {
    IconButton(
      onClick = onSeekPrevious,
      enabled = playbackItem.value?.hasPrevious ?: false,
    ) {
      Icon(
        imageVector = Icons.Rounded.SkipPrevious,
        contentDescription = "Skip to Previous", // Should be a string resource
        modifier = Modifier.size(48.dp)
      )
    }

    IconButton(
      onClick = onPlayPause,
      enabled = playPauseEnabled
    ) {
      Icon(
        imageVector = playPauseVector,
        contentDescription = "Play or Pause", // Should be a string resource
        modifier = Modifier.size(48.dp)
      )
    }

    IconButton(
      onClick = onSeekNext,
      enabled = playbackItem.value?.hasNext ?: false,
    ) {
      Icon(
        imageVector = Icons.Rounded.SkipNext,
        contentDescription = "Skip to Next", // Should be a string resource
        modifier = Modifier.size(48.dp)
      )
    }
  }
}

@Composable
private fun PlaybackPosition(
  playbackPosition: State<Long?>,
  playbackDuration: State<Long?>,
  seekPosition: State<Long?>,
  modifier: Modifier = Modifier
) {
  val positionText = remember {
    derivedStateOf {
      seekPosition.value?.millisToFormattedDuration() ?: playbackPosition.value?.millisToFormattedDuration()
    }
  }

  val durationText = remember(playbackDuration.value) {
    playbackDuration.value?.millisToFormattedDuration()
  }

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.End)
  ) {
    Text(
      text = positionText.value ?: "--"
    )

    Text(
      text = "/"
    )

    Text(
      text = durationText ?: "--"
    )
  }
}

@Composable
private fun Artwork(
  playbackItem: State<AudioPlayerViewModel.PlaybackItem?>,
  modifier: Modifier = Modifier
) {
  val artworkUrl = remember {
    derivedStateOf {
      playbackItem.value?.artworkUrl
    }
  }

  val placeholder = forwardingPainter(
    painter = rememberVectorPainter(Icons.Rounded.MusicNote),
    colorFilter = ColorFilter.tint(LocalContentColor.current),
    alpha = 0.2f
  )

  Surface(
    modifier = modifier,
    shape = DemoTheme.shapes.extraLarge,
    tonalElevation = 16.dp,
    shadowElevation = 16.dp
  ) {
    AsyncImage(
      model = artworkUrl.value,
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      placeholder = placeholder,
      error = placeholder,
      fallback = placeholder,
      contentScale = ContentScale.Crop
    )
  }
}

@Composable
private fun Modifier.playerBackground(): Modifier {
  val colorScheme = DemoTheme.colors

  // Manually drawing instead of using `background()` so that we can
  // adjust positioning
  return this.drawWithCache {
    val brush = Brush.radialGradient(
      colorStops = arrayOf(
        0f to colorScheme.secondary.copy(alpha = 0.6f),
        0.5f to colorScheme.secondary.copy(alpha = 0.6f),
        1f to colorScheme.secondary.copy(alpha = 0.25f)
      ),
      center = size.center.copy(y = size.height * 0.3f)
    )

    onDrawWithContent {
      drawRect(brush = brush)

      drawContent()
    }
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewAudioPlayerScreen() {
  val playbackState = remember { mutableStateOf(PlaybackState.PLAYING) }
  val playbackItem = remember {
    mutableStateOf(
      AudioPlayerViewModel.PlaybackItem(
        title = "Conspiracy",
        album  = "The Count of Monte Cristo",
        artworkUrl = null,
        hasNext = true,
        hasPrevious = true
      )
    )
  }

  AudioPlayerScreen(
    playbackState = playbackState,
    playbackItem = playbackItem,
    playbackPosition = remember { mutableStateOf(254_000) },
    playbackDuration = remember { mutableStateOf(490_000) },
    onBackClicked = {},
    onPlayPause = {},
    onSeekPrevious = {},
    onSeekNext = {},
    onSeek = {}
  )
}