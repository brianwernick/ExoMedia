package com.devbrackets.android.exomediademo.ui.selection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.data.Samples.Sample.Category
import com.devbrackets.android.exomediademo.ui.media.AudioPlayerActivity
import com.devbrackets.android.exomediademo.ui.media.VideoPlayerActivity
import com.devbrackets.android.exomediademo.ui.support.DemoNavHost
import com.devbrackets.android.exomediademo.util.getEnumArg
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalAnimationApi
class SelectionActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Content()
    }
  }

  @Composable
  @OptIn(ExperimentalFoundationApi::class)
  fun Content() {
    val navController = rememberAnimatedNavController()

    DemoNavHost(
      navController = navController,
      startDestination = "home",
      modifier = Modifier.background(Color(235, 235, 245)),
    ) {
      composable(
        route = "home"
      ) {
        SelectCategory(
          onCategorySelected = {
            navController.navigate("category/${it.name}")
          }
        )
      }

      composable(
        route = "category/{category}",
        arguments = listOf(
          navArgument("category") {
            type = NavType.EnumType(Category::class.java)
            defaultValue = Category.AUDIO
          }
        )
      ) { entry ->
        SelectMedia(entry.getEnumArg("category"))
      }
    }

  }

  @Composable
  private fun SelectCategory(
    onCategorySelected: (Category) -> Unit
  ) {
    CategorySelectionScreen(
      onAudioSelected = {
        onCategorySelected(Category.AUDIO)
      },
      onVideoSelected = {
        onCategorySelected(Category.VIDEO)
      }
    )
  }

  @Composable
  private fun SelectMedia(category: Category) {
    val title = when(category) {
      Category.AUDIO -> stringResource(R.string.title_audio_selection_activity)
      Category.VIDEO -> stringResource(R.string.title_video_selection_activity)
    }

    val samples = when(category) {
      Category.AUDIO -> Samples.audio
      Category.VIDEO -> Samples.video
    }

    SelectionScreen(
      title = title,
      samples = samples,
      onSampleSelected = this::playMedia
    )
  }

  private fun playMedia(sample: Samples.Sample) {
     val intent = when(sample.category) {
      Category.AUDIO -> AudioPlayerActivity.intent(this, sample)
      Category.VIDEO -> VideoPlayerActivity.intent(this, sample)
    }

    startActivity(intent)
  }

}
