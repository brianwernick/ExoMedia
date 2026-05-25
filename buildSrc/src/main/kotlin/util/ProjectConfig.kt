package util

import org.gradle.api.JavaVersion

@Suppress("ConstPropertyName")
object ProjectConfig {
    const val compileSdk = 37
    const val minSdk = 23
    const val targetSdk = compileSdk

    val javaVersion = JavaVersion.VERSION_11
}