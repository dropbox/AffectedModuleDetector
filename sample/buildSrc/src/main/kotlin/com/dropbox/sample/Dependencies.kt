package com.dropbox.sample

object Dependencies {
    private object Versions {
        const val KOTLIN_VERSION = "1.4.10"
    }

    object Libs {
        const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN_VERSION}"
        const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN_VERSION}"
        const val ANDROIDX_CORE_KTX = "androidx.core:core-ktx:1.3.2"
        const val ANDROIDX_APP_COMPAT = "androidx.appcompat:appcompat:1.2.0"
        const val ANDROID_MATERIAL = "com.google.android.material:material:1.2.1"
        const val ANDROIDX_CONSTRAINTLAYOUT = "androidx.constraintlayout:constraintlayout:1.1.3"
        const val JUNIT = "junit:junit:4.13.1"
        const val ANDROIDX_TEST_EXT = "androidx.test.ext:junit:1.1.2"
        const val ANDROIDX_ESPRESSO = "androidx.test.espresso:espresso-core:3.3.0"
        const val ANDROID_BUILD_TOOLS = "com.android.tools.build:gradle:4.1.0"
        const val KTLINT = "org.jlleitschuh.gradle:ktlint-gradle:9.1.1"
    }
}