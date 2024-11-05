package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.rules.SetupAndroidProject
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test

class AffectedModuleDetectorIntegrationTest {

    @Rule
    @JvmField
    val tmpFolder = SetupAndroidProject()

    @Test
    fun `GIVEN single project WHEN plugin is applied THEN tasks are added`() {
        // GIVEN
        // expected tasks
        val tasks = listOf(
            "runAffectedUnitTests",
            "runAffectedAndroidTests",
            "assembleAffectedAndroidTests"
        )
        tmpFolder.newFile("build.gradle").writeText(
            """plugins {
                |   id "com.dropbox.affectedmoduledetector"
                |}""".trimMargin()
        )

        // WHEN
        val result = GradleRunner.create()
            .withProjectDir(tmpFolder.root)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        // THEN
        tasks.forEach { taskName ->
            assertThat(result.output).contains(taskName)
        }
    }

    @Test
    fun `GIVEN multiple project WHEN plugin is applied THEN tasks has dependencies`() {
        // GIVEN
        tmpFolder.setupAndroidSdkLocation()
        tmpFolder.newFolder("sample-app")
        tmpFolder.newFolder("sample-core")
        tmpFolder.newFile("settings.gradle").writeText(
            """
                |include ':sample-app'
                |include ':sample-core'
                """.trimMargin()
        )

        tmpFolder.newFile("build.gradle").writeText(
            """buildscript {
                |   repositories {
                |       google()
                |       jcenter()
                |   }
                |   dependencies {
                |       classpath "com.android.tools.build:gradle:7.4.0"
                |       classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25"
                |   }    
                |}
                |plugins {
                |   id "com.dropbox.affectedmoduledetector"
                |}
                |allprojects {
                |   repositories {
                |       google()
                |       jcenter()
                |   }
                |}""".trimMargin()
        )

        tmpFolder.newFile("sample-app/build.gradle").writeText(
            """plugins {
                |     id 'com.android.application'
                |     id 'kotlin-android'
                |   }
                |   android {
                |       compileSdkVersion 33
                |       namespace "sample"
                |   }
                |   dependencies {
                |     implementation project(":sample-core")
                |   }""".trimMargin()
        )

        tmpFolder.newFolder("sample-app/src/main/")
        tmpFolder.newFile("sample-app/src/main/AndroidManifest.xml").writeText(
            """
                |<manifest>
                |</manifest>
                """.trimMargin()
        )

        tmpFolder.newFile("sample-core/build.gradle").writeText(
            """plugins {
                |   id 'com.android.library'
                |   id 'kotlin-android'
                |   }
                |   affectedTestConfiguration {
                |       assembleAndroidTestTask = "assembleAndroidTest"
                |   }
                |   android {
                |       namespace 'sample.core'
                |       compileSdkVersion 33
                |   }""".trimMargin()
        )

        tmpFolder.newFolder("sample-core/src/main/")
        tmpFolder.newFile("sample-core/src/main/AndroidManifest.xml").writeText(
            """
                |<manifest>
                |</manifest>
                """.trimMargin()
        )

        // WHEN
        val result = GradleRunner.create()
            .withProjectDir(tmpFolder.root)
            .withPluginClasspath()
            .withArguments("assembleAffectedAndroidTests", "--dry-run")
            .build()

        // THEN
        assertThat(result.output).contains(":sample-app:assembleDebugAndroidTest SKIPPED")
        assertThat(result.output).contains(":sample-core:mergeDexDebugAndroidTest SKIPPED")
        assertThat(result.output).contains(":sample-core:packageDebugAndroidTest SKIPPED")
        assertThat(result.output).contains(":sample-core:assembleDebugAndroidTest SKIPPED")
        assertThat(result.output).contains(":sample-core:assembleAndroidTest SKIPPED")
        assertThat(result.output).contains(":assembleAffectedAndroidTests SKIPPED")
    }

    @Test
    fun `GIVEN multiple project with one excluded WHEN plugin is applied THEN tasks has dependencies minus the exclusions`() {
        // GIVEN
        tmpFolder.newFolder("sample-app")
        tmpFolder.newFolder("sample-core")
        tmpFolder.newFile("settings.gradle").writeText(
            """
                |include ':sample-app'
                |include ':sample-core'
                """.trimMargin()
        )

        tmpFolder.newFile("build.gradle").writeText(
            """buildscript {
                |   repositories {
                |       google()
                |       jcenter()
                |   }
                |   dependencies {
                |       classpath "com.android.tools.build:gradle:7.4.0"
                |       classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25"
                |   }    
                |}
                |plugins {
                |   id "com.dropbox.affectedmoduledetector"
                |}
                |affectedModuleDetector {
                |   excludedModules = [ "sample-core" ]
                |}
                |allprojects {
                |   repositories {
                |       google()
                |       jcenter()
                |   }
                |}""".trimMargin()
        )

        tmpFolder.newFile("sample-app/build.gradle").writeText(
            """plugins {
                |     id 'com.android.application'
                |     id 'kotlin-android'
                |   }
                |   android {
                |       namespace 'sample'
                |       compileSdkVersion 33
                |   }
                |   dependencies {
                |     implementation project(":sample-core")
                |   }""".trimMargin()
        )

        tmpFolder.newFolder("sample-app/src/main/")
        tmpFolder.newFile("sample-app/src/main/AndroidManifest.xml").writeText(
            """
                |<manifest>
                |</manifest>
                """.trimMargin()
        )

        tmpFolder.newFile("sample-core/build.gradle").writeText(
            """plugins {
                |   id 'com.android.library'
                |   id 'kotlin-android'
                |   }
                |   affectedTestConfiguration {
                |       assembleAndroidTestTask = "assembleAndroidTest"
                |   }
                |   android {
                |       namespace 'sample.core'
                |       compileSdkVersion 33
                |   }""".trimMargin()
        )

        tmpFolder.newFolder("sample-core/src/main/")
        tmpFolder.newFile("sample-core/src/main/AndroidManifest.xml").writeText(
            """
                |<manifest>
                |</manifest>
                """.trimMargin()
        )

        // WHEN
        val result = GradleRunner.create()
            .withProjectDir(tmpFolder.root)
            .withPluginClasspath()
            .withArguments("assembleAffectedAndroidTests", "--dry-run")
            .build()

        // THEN
        assertThat(result.output).contains(":sample-app:assembleDebugAndroidTest SKIPPED")
        assertThat(result.output).doesNotContain(":sample-core:mergeDexDebugAndroidTest")
        assertThat(result.output).doesNotContain(":sample-core:packageDebugAndroidTest")
        assertThat(result.output).doesNotContain(":sample-core:assembleDebugAndroidTest")
        assertThat(result.output).doesNotContain(":sample-core:assembleAndroidTest")
        assertThat(result.output).contains(":assembleAffectedAndroidTests SKIPPED")
    }
}
