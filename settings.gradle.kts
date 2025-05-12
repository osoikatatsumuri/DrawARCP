pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Android Gradle Plugin
        id("com.android.application") version "8.8.2"
        // Kotlin Android Plugin (KGP)
        id("org.jetbrains.kotlin.android")    version "2.1.10"
        // KSP: Kotlin Symbol Processing
        id("com.google.devtools.ksp")        version "2.1.10-1.0.31"

        id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DrawARCP"
include(":app")
 