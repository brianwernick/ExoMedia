package com.devbrackets.android.exomediademo.ui.support.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScreenScaffold(
  title: String,
  subTitle: String? = null,
  onBackClick: (() -> Unit)? = null,
  content: @Composable (PaddingValues) -> Unit
) {
  DemoTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
              )
              AnimatedContent(
                targetState = subTitle,
              ) { target ->
                target?.let {
                  Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = DemoTheme.typography.bodyMedium
                  )
                }
              }
            }
          },
          navigationIcon = {
            onBackClick?.let {
              IconButton(
                onClick = it,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = null
                )
              }
            }
          },
          windowInsets = WindowInsets.statusBars
        )
      },
      content = content
    )
  }
}

@Preview
@Composable
private fun PreviewScreenScaffold() {
  ScreenScaffold(
    title = "ExoMedia Demo App",
    subTitle = "Sub-title",
    onBackClick = {}
  ) { }
}