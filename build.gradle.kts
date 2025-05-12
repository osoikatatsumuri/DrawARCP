// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.31" apply false
    id("org.jetbrains.kotlin.jvm") version "1.6.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0" apply false
}