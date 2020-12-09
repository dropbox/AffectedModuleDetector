package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.fail
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AffectedModuleDetectorPluginTest {

    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    lateinit var rootProject: Project
    lateinit var childProject : Project

    @Before
    fun setup() {
        rootProject = ProjectBuilder.builder()
            .withName("root")
            .withProjectDir(tmpFolder.root)
            .build()

        childProject = ProjectBuilder.builder()
            .withName("child")
            .withParent(rootProject)
            .build()
    }

    @Test
    fun `GIVEN child project WHEN plugin is applied THEN throw exception`() {
        // GIVEN
        rootProject.pluginManager.apply(AffectedModuleDetectorPlugin::class.java)

        try {
            // WHEN
            childProject.pluginManager.apply(AffectedModuleDetectorPlugin::class.java)
            fail("Expected exception not thrown")
        } catch (e: Exception) {
            // THEN
            assertThat(e.message).isEqualTo("Failed to apply plugin [class 'com.dropbox.affectedmoduledetector.AffectedModuleDetectorPlugin']")
        }
    }

    @Test
    fun `GIVEN root project WHEN plugin is applied THEN extensions are added`() {
        // GIVEN
        // root project

        // WHEN
        rootProject.pluginManager.apply(AffectedModuleDetectorPlugin::class.java)
        val extension = rootProject.extensions.findByName("affectedModuleDetector")

        // THEN
        assertThat(extension).isNotNull()
        assertThat(extension).isInstanceOf(AffectedModuleConfiguration::class.java)
    }

    @Test
    fun `GIVEN root project WHEN plugin is applied THEN tasks are added`() {
        // GIVEN
        // expected tasks
        val tasks = listOf(
            "runAffectedUnitTests",
            "runAffectedAndroidTests",
            "assembleAffectedAndroidTests"
        )
        writeBuildGradle(
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

    private fun writeBuildGradle(build: String) {
        tmpFolder.newFile("build.gradle")
                .writeText(build)
    }
}
