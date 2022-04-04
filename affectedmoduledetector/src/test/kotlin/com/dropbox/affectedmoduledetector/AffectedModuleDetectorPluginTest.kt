package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.plugin.AffectedModuleDetectorPlugin
import com.dropbox.affectedmoduledetector.plugin.AffectedModuleTaskType
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.IllegalStateException

class AffectedModuleDetectorPluginTest {

    private companion object {

        const val FAKE_COMMAND_BY_IMPACT = "fake_command"
        const val FAKE_ORIGINAL_COMMAND = "fake_original_gradle_command"
        const val FAKE_TASK_DESCRIPTION = "fake_description"
    }

    private enum class FakeTaskType(
        override val commandByImpact: String,
        override val originalGradleCommand: String,
        override val taskDescription: String
    ): AffectedModuleTaskType {

        FAKE_TASK(
            commandByImpact = FAKE_COMMAND_BY_IMPACT,
            originalGradleCommand = FAKE_ORIGINAL_COMMAND,
            taskDescription = FAKE_TASK_DESCRIPTION
        )
    }

    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    lateinit var rootProject: Project
    lateinit var childProject: Project

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
            throw IllegalStateException("Expected to throw exception")
        } catch (e: PluginApplicationException) {
            // THEN
            assertThat(e.message).isEqualTo("Failed to apply plugin class 'com.dropbox.affectedmoduledetector.plugin.AffectedModuleDetectorPlugin'.")
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
    fun `GIVEN affected module detector plugin WHEN register task is called THEN task is added`() {
        // GIVEN
        val task = FakeTaskType.FAKE_TASK
        val plugin = AffectedModuleDetectorPlugin()

        // WHEN
        plugin.registerAffectedTestTask(
            rootProject = rootProject,
            taskType = task,
            groupName = "fakeGroup"
        )
        val result = rootProject.tasks.findByPath(task.commandByImpact)

        // THEN
        assertThat(result).isNotNull()
        assertThat(result?.name).isEqualTo(FakeTaskType.FAKE_TASK.commandByImpact)
        assertThat(result?.group).isEqualTo("fakeGroup")
        assertThat(result?.description).isEqualTo(FakeTaskType.FAKE_TASK.taskDescription)
    }
}
