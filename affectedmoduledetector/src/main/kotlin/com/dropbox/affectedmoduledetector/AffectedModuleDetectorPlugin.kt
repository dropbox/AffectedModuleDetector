/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.affectedmoduledetector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.impldep.org.jetbrains.annotations.VisibleForTesting
import org.gradle.util.GradleVersion

/**
 * This plugin creates and registers all affected test tasks.
 * Advantage is speed in not needing to skip modules at a large scale.
 *
 * Registers 3 tasks:
 *     - `gradlew runAffectedUnitTests` - runs jvm tests
 *     - `gradlew runAffectedAndroidTests` - runs connected tests
 *     - `gradlew assembleAffectedAndroidTests` - assembles but does not run on device tests,
 * useful when working with device labs.
 *
 * Configure using affected module detector block after applying the plugin:
 *
 *   affectedModuleDetector {
 *       baseDir = "${project.rootDir}"
 *       pathsAffectingAllModules = [
 *           "buildSrc/"
 *       ]
 *       logFolder = "${project.rootDir}".
 *   }
 *
 * To enable affected module detection, you need to pass [ENABLE_ARG]
 * into the build as a command line parameter.
 *
 * See [AffectedModuleDetector] for additional flags.
 */
class AffectedModuleDetectorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        require(
            value = project.isRoot,
            lazyMessage = {
                "Must be applied to root project, but was found on ${project.path} instead."
            }
        )

        registerSubprojectConfiguration(project)
        registerMainConfiguration(project)
        registerCustomTasks(project)
        registerTestTasks(project)

        project.gradle.projectsEvaluated {
            AffectedModuleDetector.configure(project.gradle, project)

            filterAndroidTests(project)
            filterJvmTests(project)
            filterCustomTasks(project)
        }
    }

    private fun registerMainConfiguration(project: Project) {
        project.extensions.add(
            AffectedModuleConfiguration.name,
            AffectedModuleConfiguration()
        )
    }

    private fun registerSubprojectConfiguration(project: Project) {
        project.subprojects { subproject ->
            subproject.extensions.add(
                AffectedTestConfiguration.name,
                AffectedTestConfiguration()
            )
        }
    }

    private fun registerCustomTasks(rootProject: Project) {
        val mainConfiguration = requireNotNull(
            value = rootProject.extensions.findByName(AffectedModuleConfiguration.name),
            lazyMessage = {  "Unable to find ${AffectedTestConfiguration.name} in $rootProject" }
        ) as AffectedModuleConfiguration

        rootProject.afterEvaluate {
            registerCustomTasks(rootProject, mainConfiguration.customTasks)
        }
    }

    @VisibleForTesting
    internal fun registerCustomTasks(
        rootProject: Project,
        customTasks: Set<AffectedModuleTaskType>
    ) {
        customTasks.forEach { taskType ->
            rootProject.tasks.register(taskType.commandByImpact) { task ->
                task.group = CUSTOM_TASK_GROUP_NAME
                task.description = taskType.taskDescription
                disableConfigCache(task)

                rootProject.subprojects { project ->
                    pluginIds.forEach { pluginId ->
                        withPlugin(pluginId, task, taskType, project)
                    }
                }
            }
        }
    }

    @VisibleForTesting
    internal fun registerTestTasks(rootProject: Project) {
        registerInternalTask(
            rootProject = rootProject,
            taskType = InternalTaskType.ANDROID_JVM_TEST,
            groupName = TEST_TASK_GROUP_NAME
        )

        registerInternalTask(
            rootProject = rootProject,
            taskType = InternalTaskType.ANDROID_TEST,
            groupName = TEST_TASK_GROUP_NAME
        )

        registerInternalTask(
            rootProject = rootProject,
            taskType = InternalTaskType.ASSEMBLE_ANDROID_TEST,
            groupName = TEST_TASK_GROUP_NAME
        )
    }

    @Suppress("UnstableApiUsage")
    @VisibleForTesting
    internal fun registerInternalTask(
        rootProject: Project,
        taskType: AffectedModuleTaskType,
        groupName: String
    ) {
        rootProject.tasks.register(taskType.commandByImpact) { task ->
            task.group = groupName
            task.description = taskType.taskDescription
            disableConfigCache(task)

            rootProject.subprojects { project ->
                pluginIds.forEach { pluginId ->
                    if (pluginId == PLUGIN_JAVA_LIBRARY || pluginId == PLUGIN_KOTLIN) {
                        if (taskType == InternalTaskType.ANDROID_JVM_TEST) {
                            withPlugin(pluginId, task, InternalTaskType.JVM_TEST, project)
                        }
                    } else {
                        withPlugin(pluginId, task, taskType, project)
                    }
                }
            }
        }
    }

    private fun disableConfigCache(task: Task) {
        if (GradleVersion.current() >= GradleVersion.version("7.4")) {
            task.notCompatibleWithConfigurationCache("AMD requires knowledge of what has changed in the file system so we can not cache those values (https://github.com/dropbox/AffectedModuleDetector/issues/150)")
        }
    }

    private fun withPlugin(
        pluginId: String,
        task: Task,
        testType: AffectedModuleTaskType,
        project: Project
    ) {
        project.pluginManager.withPlugin(pluginId) {
            getAffectedPath(testType, project)?.let { path ->
                if (AffectedModuleDetector.isProjectProvided(project)) {
                    task.dependsOn(path)
                }

                project.tasks.findByPath(path)?.onlyIf { task ->
                    when {
                        !AffectedModuleDetector.isProjectEnabled(task.project) -> true
                        else -> AffectedModuleDetector.isProjectAffected(task.project)
                    }
                }
            }
        }
    }

    private fun getAffectedPath(
        taskType: AffectedModuleTaskType,
        project: Project
    ): String? {
        val tasks = requireNotNull(
            value = project.extensions.findByName(AffectedTestConfiguration.name),
            lazyMessage = { "Unable to find ${AffectedTestConfiguration.name} in $project" }
        ) as AffectedTestConfiguration

        return when (taskType) {
            InternalTaskType.ANDROID_TEST -> {
                getPathAndTask(project, tasks.runAndroidTestTask)
            }
            InternalTaskType.ASSEMBLE_ANDROID_TEST -> {
                getPathAndTask(project, tasks.assembleAndroidTestTask)
            }
            InternalTaskType.ANDROID_JVM_TEST -> {
                getPathAndTask(project, tasks.jvmTestTask)
            }
            InternalTaskType.JVM_TEST -> {
                if (tasks.jvmTestTask != AffectedTestConfiguration.DEFAULT_JVM_TEST_TASK) {
                    getPathAndTask(project, tasks.jvmTestTask)
                } else {
                    getPathAndTask(project, taskType.originalGradleCommand)
                }
            }
            else -> {
                getPathAndTask(project, taskType.originalGradleCommand)
            }
        }
    }

    private fun getPathAndTask(project: Project, task: String?): String? {
        return if (task.isNullOrBlank()) null else "${project.path}:${task}"
    }

    private fun filterAndroidTests(project: Project) {
        val tracker = DependencyTracker(project, null)
        project.tasks.configureEach { task ->
            if (task.name.contains(ANDROID_TEST_PATTERN)) {
                tracker.findAllDependents(project).forEach { dependentProject ->
                    dependentProject.tasks.forEach { dependentTask ->
                        AffectedModuleDetector.configureTaskGuard(dependentTask)
                    }
                }
                AffectedModuleDetector.configureTaskGuard(task)
            }
        }
    }

    private fun filterCustomTasks(project: Project) {
        project.tasks.configureEach { task ->
            if (task.group == CUSTOM_TASK_GROUP_NAME) {
                AffectedModuleDetector.configureTaskGuard(task)
            }
        }
    }

    // Only allow unit tests to run if the AffectedModuleDetector says to include them
    private fun filterJvmTests(project: Project) {
        project.tasks.withType(Test::class.java).configureEach { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }
    }

    companion object {

        @VisibleForTesting
        internal const val TEST_TASK_GROUP_NAME = "Affected Module Detector"
        @VisibleForTesting
        internal const val CUSTOM_TASK_GROUP_NAME = "Affected Module Detector custom tasks"

        private const val PLUGIN_ANDROID_APPLICATION = "com.android.application"
        private const val PLUGIN_ANDROID_LIBRARY = "com.android.library"
        private const val PLUGIN_JAVA_LIBRARY = "java"
        private const val PLUGIN_KOTLIN = "kotlin"

        private const val ANDROID_TEST_PATTERN = "AndroidTest"

        private val pluginIds = listOf(
            PLUGIN_ANDROID_APPLICATION,
            PLUGIN_ANDROID_LIBRARY,
            PLUGIN_JAVA_LIBRARY,
            PLUGIN_KOTLIN
        )
    }
}
