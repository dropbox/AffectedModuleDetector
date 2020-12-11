package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AffectedModuleDetectorIntegrationTest {

    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

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
                |       classpath "com.android.tools.build:gradle:4.1.0"
                |       classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10"
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
                |       compileSdkVersion 30
                |       buildToolsVersion "30.0.2"
                |   }
                |   dependencies {
                |     implementation project(":sample-core")
                |   }""".trimMargin()
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
                |       compileSdkVersion 30
                |       buildToolsVersion "30.0.2"
                |   }""".trimMargin()
        )

        // WHEN
        val result = GradleRunner.create()
                .withProjectDir(tmpFolder.root)
                .withPluginClasspath()
                .withArguments("assembleAffectedAndroidTests", "--dry-run")
                .build()

        // THEN
        assertThat(result.output).contains(":sample-app:assembleDebugAndroidTest SKIPPED")
        assertThat(result.output).contains(":sample-core:assembleAndroidTest SKIPPED")
        assertThat(result.output).contains(":assembleAffectedAndroidTests SKIPPED")
    }
}