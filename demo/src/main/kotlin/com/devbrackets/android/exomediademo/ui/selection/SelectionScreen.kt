package com.devbrackets.android.exomediademo.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.ui.support.compose.ScreenScaffold
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme

@Composable
fun CategorySelectionScreen(
  onAudioSelected: () -> Unit,
  onVideoSelected: () -> Unit
) {
  ScreenScaffold(
    title = stringResource(R.string.app_name)
  ) { padding ->
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
      horizontalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterHorizontally)
    ) {
      item {
        MediaCategoryCard(
          title = "Audio",
          image = Icons.Rounded.Audiotrack,
          onClick = onAudioSelected
        )
      }

      item {
        MediaCategoryCard(
          title = "Video",
          image = Icons.Rounded.Videocam,
          onClick = onVideoSelected
        )
      }
    }
  }
}

@Composable
fun SelectionScreen(
  title: String,
  samples: List<Samples.Sample>,
  onBackClicked: () -> Unit,
  onSampleSelected: (Samples.Sample) -> Unit
) {
  ScreenScaffold(
    title = title,
    onBackClick = onBackClicked
  ) { padding ->
    LazyColumn(
      modifier = Modifier.padding(padding),
      contentPadding = PaddingValues(
        top = 8.dp,
        bottom = 56.dp
      )
    ) {
      items(samples) { sample ->
        MediaItem(
          title = sample.title,
          onClick = {
            onSampleSelected(sample)
          }
        )
      }
    }
  }
}

@Composable
private fun MediaItem(
  title: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 48.dp)
      .padding(horizontal = 8.dp)
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 16.dp)
  ) {
    Text(text = title)
  }
}

@Composable
fun MediaCategoryCard(
  title: String,
  image: ImageVector,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    modifier = Modifier.size(124.dp),
    elevation = CardDefaults.elevatedCardElevation()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      Icon(
        imageVector = image,
        contentDescription = null,
        modifier = Modifier
          .padding(bottom = 24.dp)
          .size(48.dp)
          .align(Alignment.Center),
        tint = LocalContentColor.current.copy(alpha = 0.4f)
      )

      Text(
        text = title,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = DemoTheme.typography.titleLarge
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewSelectionScreen() {
  SelectionScreen(
    title = "Select a video",
    samples = Samples.video,
    onBackClicked = {},
    onSampleSelected = {}
  )
}