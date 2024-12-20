package com.devbrackets.android.exomediademo.ui.selection

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.data.Samples.Sample.Category
import com.devbrackets.android.exomediademo.ui.media.AudioPlayerActivity
import com.devbrackets.android.exomediademo.ui.media.VideoPlayerActivity
import com.devbrackets.android.exomediademo.ui.support.compose.DemoNavHost
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme
import com.devbrackets.android.exomediademo.util.getEnumArg

@ExperimentalAnimationApi
class SelectionActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Content()
    }
  }

  @Composable
  fun Content() {
    val navController = rememberNavController()

    DemoNavHost(
      navController = navController,
      startDestination = "home",
      modifier = Modifier.background(DemoTheme.colors.background),
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
        SelectMedia(
          category = entry.getEnumArg("category"),
          onBackClicked = {
            navController.popBackStack()
          }
        )
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
  private fun SelectMedia(
    category: Category,
    onBackClicked: () -> Unit,
  ) {
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
      onBackClicked = onBackClicked,
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
