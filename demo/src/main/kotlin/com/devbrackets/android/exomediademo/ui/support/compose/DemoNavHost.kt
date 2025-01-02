package com.devbrackets.android.exomediademo.ui.support.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.devbrackets.android.exomediademo.ui.support.compose.theme.DemoTheme

private const val ANIMATION_DURATION = 300

private val enterTransition = slideInHorizontally(
  initialOffsetX = { it },
  animationSpec = tween(
    durationMillis = ANIMATION_DURATION,
    easing = FastOutSlowInEasing
  )
)

private val exitTransition = slideOutHorizontally(
  targetOffsetX = { -it / 5 },
  animationSpec = tween(
    durationMillis = ANIMATION_DURATION,
    easing = FastOutSlowInEasing
  )
)

private val popEnterTransition = slideInHorizontally(
  initialOffsetX = { -it },
  animationSpec = tween(
    durationMillis = ANIMATION_DURATION,
    easing = FastOutSlowInEasing
  )
)

private val popExitTransition = slideOutHorizontally(
  targetOffsetX = { it },
  animationSpec = tween(
    durationMillis = ANIMATION_DURATION,
    easing = FastOutSlowInEasing
  )
)

@Composable
fun DemoNavHost(
  navController: NavHostController,
  startDestination: String,
  modifier: Modifier = Modifier,
  route: String? = null,
  builder: NavGraphBuilder.() -> Unit
) {
  DemoTheme {
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = modifier,
      route = route,
      enterTransition = { enterTransition },
      exitTransition = { exitTransition },
      popEnterTransition = { popEnterTransition },
      popExitTransition = { popExitTransition },
      builder = builder
    )
  }
}