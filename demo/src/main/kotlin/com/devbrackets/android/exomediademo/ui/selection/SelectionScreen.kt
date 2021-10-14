package com.devbrackets.android.exomediademo.ui.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.Samples

@Preview(showBackground = true)
@Composable
private fun PreviewSelectionScreen() {
  SelectionScreen(
    title = "Select a video",
    samples = Samples.video,
    onSampleSelected = {}
  )
}

@Composable
fun SelectionScreenFrame(
  title: String,
  content: @Composable (PaddingValues) -> Unit
) {
  MaterialTheme(
    colors = MaterialTheme.colors.copy(
      primary = Color(66, 165, 245),
      primaryVariant = Color(96, 125, 139),
      secondary = Color(96, 125, 139),
      background = Color(235, 235, 245)
    )
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(title)
          },
          backgroundColor = MaterialTheme.colors.primaryVariant
        )
      },
      content = content
    )
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CategorySelectionScreen(
  onAudioSelected: () -> Unit,
  onVideoSelected: () -> Unit
) {
  SelectionScreenFrame(
    title = stringResource(R.string.app_name)
  ) {
    LazyVerticalGrid(
      cells = GridCells.Fixed(2),
      contentPadding = PaddingValues(16.dp)
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
  onSampleSelected: (Samples.Sample) -> Unit
) {
  SelectionScreenFrame(
    title = title
  ) {
    LazyColumn {
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
      .clickable {
        onClick()
      }
      .padding(16.dp)
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
  Box {
    Card(
      modifier = Modifier
        .size(136.dp, 136.dp)
        .clickable(onClick = onClick)
        .align(Alignment.Center),
      elevation = 2.dp
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
          tint = Color(235, 235, 245)
        )

        Text(
          text = title,
          modifier = Modifier.align(Alignment.BottomStart),
          fontSize = 24.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}