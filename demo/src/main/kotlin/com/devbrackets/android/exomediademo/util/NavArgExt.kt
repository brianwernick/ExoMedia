package com.devbrackets.android.exomediademo.util

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import java.lang.IllegalArgumentException

fun <T: Enum<T>> NavBackStackEntry.getEnumArg(name: String): T {
  val bundle = arguments ?: throw IllegalArgumentException("Cannot retrieve enum for name \"$name\" without a Bundle")
  val type = destination.arguments[name]?.type

  if (type == null || type !is NavType.EnumType<*>) {
    throw IllegalArgumentException("Cannot retrieve enum for name \"$name\" due to a type mismatch")
  }

  return type[bundle, name] as T
}