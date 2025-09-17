package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AffectedModuleDetectorPluginTest {

    private companion object {

        const val FAKE_COMMAND_BY_IMPACT = "fake_command"
        const val FAKE_ORIGINAL_COMMAND = "fake_original_gradle_command"
        const val FAKE_TASK_DESCRIPTION = "fake_description"
    }

    private val fakeTask = AffectedModuleConfiguration.CustomTask(
        commandByImpact = FAKE_COMMAND_BY_IMPACT,
        originalGradleCommand = FAKE_ORIGINAL_COMMAND,
        taskDescription = FAKE_TASK_DESCRIPTION
    )

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
            assertThat(e.message).isEqualTo("Failed to apply plugin class 'com.dropbox.affectedmoduledetector.AffectedModuleDetectorPlugin'.")
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
        val task = fakeTask
        val plugin = AffectedModuleDetectorPlugin()
        rootProject.pluginManager.apply(AffectedModuleDetectorPlugin::class.java)

        // WHEN
        plugin.registerInternalTask(
            rootProject = rootProject,
            taskType = task,
            groupName = "fakeGroup"
        )
        val result = rootProject.tasks.findByPath(task.commandByImpact)

        // THEN
        assertThat(result).isNotNull()
        assertThat(result?.name).isEqualTo(fakeTask.commandByImpact)
        assertThat(result?.group).isEqualTo("fakeGroup")
        assertThat(result?.description).isEqualTo(fakeTask.taskDescription)
    }

    @Test
    fun `GIVEN affected module detector plugin WHEN register_custom_task is called AND AffectedModuleConfiguration customTask is not empty THEN task is added`() {
        // GIVEN
        val configuration = AffectedModuleConfiguration()
        configuration.customTasks = setOf(fakeTask)
        rootProject.extensions.add(AffectedModuleConfiguration.name, configuration)

        val plugin = AffectedModuleDetectorPlugin()

        // WHEN
        plugin.registerCustomTasks(rootProject, setOf(fakeTask))
        val result = rootProject.tasks.findByPath(fakeTask.commandByImpact)

        // THEN
        assertThat(result).isNotNull()
        assertThat(result?.name).isEqualTo(fakeTask.commandByImpact)
        assertThat(result?.group).isEqualTo(AffectedModuleDetectorPlugin.CUSTOM_TASK_GROUP_NAME)
        assertThat(result?.description).isEqualTo(fakeTask.taskDescription)
    }

    @Test
    fun `GIVEN affected module detector plugin WHEN registerCustomTasks is called AND AffectedModuleConfiguration customTask is empty THEN task isn't added`() {
        // GIVEN
        val configuration = AffectedModuleConfiguration()
        rootProject.extensions.add(AffectedModuleConfiguration.name, configuration)
        val plugin = AffectedModuleDetectorPlugin()

        // WHEN
        plugin.registerCustomTasks(rootProject, emptySet())
        val result = rootProject
            .tasks
            .filter { it.group == AffectedModuleDetectorPlugin.CUSTOM_TASK_GROUP_NAME }

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN AffectedModuleDetectorPlugin WHEN CUSTOM_TASK_GROUP_NAME compared with TEST_TASK_GROUP_NAME THEN they isn't equal`() {
        assert(AffectedModuleDetectorPlugin.CUSTOM_TASK_GROUP_NAME != AffectedModuleDetectorPlugin.TEST_TASK_GROUP_NAME)
    }

    @Test
    fun `GIVEN affected module detector plugin WHEN registerTestTasks THEN task all task added`() {
        // GIVEN
        val configuration = AffectedModuleConfiguration()
        rootProject.extensions.add(AffectedModuleConfiguration.name, configuration)
        val plugin = AffectedModuleDetectorPlugin()

        // WHEN
        plugin.registerTestTasks(rootProject)
        val androidTestTask = rootProject.tasks.findByPath(InternalTaskType.ANDROID_TEST.commandByImpact)
        val assembleAndroidTestTask = rootProject.tasks.findByPath(InternalTaskType.ASSEMBLE_ANDROID_TEST.commandByImpact)
        val jvmTestTask = rootProject.tasks.findByPath(InternalTaskType.ANDROID_JVM_TEST.commandByImpact)

        // THEN
        assertThat(androidTestTask).isNotNull()
        assertThat(androidTestTask?.group).isEqualTo(AffectedModuleDetectorPlugin.TEST_TASK_GROUP_NAME)

        assertThat(assembleAndroidTestTask).isNotNull()
        assertThat(assembleAndroidTestTask?.group).isEqualTo(AffectedModuleDetectorPlugin.TEST_TASK_GROUP_NAME)

        assertThat(jvmTestTask).isNotNull()
        assertThat(jvmTestTask?.group).isEqualTo(AffectedModuleDetectorPlugin.TEST_TASK_GROUP_NAME)
    }

    @Test
    fun `GIVEN affected module detector plugin WHEN registerTestTasks called THEN added all tasks from InternalTaskType`() {
        // GIVEN
        val configuration = AffectedModuleConfiguration()
        rootProject.extensions.add(AffectedModuleConfiguration.name, configuration)
        val plugin = AffectedModuleDetectorPlugin()
        val availableTaskVariants = 3 // runAffectedAndroidTests, assembleAffectedAndroidTests and runAffectedUnitTests

        // WHEN
        plugin.registerTestTasks(rootProject)
        val testTasks = rootProject
            .tasks
            .filter { it.group == AffectedModuleDetectorPlugin.TEST_TASK_GROUP_NAME }

        // THEN
        assert(testTasks.size == availableTaskVariants)
    }

    @Test
    fun `GIVEN affected module detector plugin WHEN registerCustomTasks called THEN added all tasks from FakeTaskType`() {
        // GIVEN
        val givenCustomTasks = setOf(fakeTask, fakeTask.copy(commandByImpact = "otherCommand"))
        val configuration = AffectedModuleConfiguration()
        configuration.customTasks = givenCustomTasks
        rootProject.extensions.add(AffectedModuleConfiguration.name, configuration)
        val plugin = AffectedModuleDetectorPlugin()

        // WHEN
        plugin.registerCustomTasks(rootProject, givenCustomTasks)

        val customTasks = rootProject
            .tasks
            .filter { it.group == AffectedModuleDetectorPlugin.CUSTOM_TASK_GROUP_NAME }

        // THEN
        assert(customTasks.size == 2)
    }
}
