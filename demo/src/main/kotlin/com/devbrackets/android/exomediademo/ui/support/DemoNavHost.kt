package com.devbrackets.android.exomediademo.ui.support

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost

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
@OptIn(ExperimentalAnimationApi::class)
fun DemoNavHost(
  navController: NavHostController,
  startDestination: String,
  modifier: Modifier = Modifier,
  route: String? = null,
  builder: NavGraphBuilder.() -> Unit
) {
  AnimatedNavHost(
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