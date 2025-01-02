package com.devbrackets.android.exomediademo.ui.support.compose.seek

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme
import kotlin.math.roundToLong

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun Seekbar(
  position: State<Long?>,
  maxValue: State<Long?>,
  enabled: Boolean,
  onSeek: (SeekState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val seekState = remember { mutableStateOf<SeekState?>(null) }
  val derivedValue = remember {
    derivedStateOf {
      determineSeekbarValue(position.value, maxValue.value, seekState.value)
    }
  }

  Slider(
    value = derivedValue.value,
    onValueChange = {
      determineSeekPosition(it, maxValue = maxValue.value)?.let { seekValue ->
        val newState = when (seekState.value) {
            null -> SeekState.Started(seekValue)
            else -> SeekState.Seeking(seekValue)
        }

        seekState.value = newState
        onSeek(newState)
      }
    },
    modifier = modifier,
    enabled = enabled,
    onValueChangeFinished = {
      seekState.value?.let {
        onSeek(SeekState.Finished(it.position))
      }

      seekState.value = null
    },
    track = { sliderState ->
      SliderDefaults.Track(
        colors = SliderDefaults.colors(),
        drawStopIndicator = null,
        sliderState = sliderState
      )
    },
  )
}

@Immutable
sealed interface SeekState {
  val position: Long

  @Immutable
  data class Started(override val position: Long): SeekState

  @Immutable
  data class Seeking(override val position: Long): SeekState

  @Immutable
  data class Finished(override val position: Long): SeekState
}

@Stable
private fun determineSeekbarValue(
  position: Long?,
  maxValue: Long?,
  seekState: SeekState?
): Float {
  if (position == null || maxValue == null || maxValue <= 0) {
    return 0f
  }

  val displayPosition = when (seekState) {
    is SeekState.Started, is SeekState.Seeking -> seekState.position
    else -> position
  }

  return (displayPosition.toDouble() / maxValue).toFloat()
}

@Stable
private fun determineSeekPosition(
  interactionValue: Float?,
  maxValue: Long?
): Long? {
  if (interactionValue == null || maxValue == null) {
    return null
  }

  return (interactionValue.toDouble() * maxValue).roundToLong()
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewSeekbar() {
  val position = remember { mutableStateOf<Long?>(254_000) }

  DemoTheme {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      Seekbar(
        position = position,
        maxValue = remember { mutableStateOf(490_000) },
        enabled = true,
        onSeek = {
          position.value = it.position
        }
      )
    }
  }
}
