import util.TestOutput

buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.kotlin.gradle)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.nexus.publish)
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }

    tasks.withType(Test::class).configureEach {
        TestOutput.configure(this)
    }
}

/*
 * Below is the functionality to publish the `library` module to Maven Central (Sonatype) using the
 * plugin `io.github.gradle-nexus.publish-plugin`. This functionality is included in this (root)
 * `build.gradle` because that's what the plugin expects; if this changes in the future then we
 * should migrate this functionality into the `publish.gradle` file instead.
 */
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = System.getenv("SONATYPE_TOKEN_USERNAME")
            password = System.getenv("SONATYPE_TOKEN_PASSWORD")
        }
    }
}