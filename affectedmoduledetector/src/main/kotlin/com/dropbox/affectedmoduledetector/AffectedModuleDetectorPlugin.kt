/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.affectedmoduledetector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

/**
 * This plugin creates and registers all affected test tasks.
 * Advantage is speed in not needing to skip modules at a large scale
 *
 *
 * Registers 3 tasks
 * gradlew runAffectedUnitTests - runs jvm tests
 * gradlew runAffectedAndroidTests - runs connected tests
 * gradlew assembleAffectedAndroidTests - assembles but does not run on device tests, useful when working with device labs
 *
 * configure using affected module detector block after applying the plugin
 *
 *   affectedModuleDetector {
 *     baseDir = "${project.rootDir}"
 *     pathsAffectingAllModules = [
 *         "buildSrc/"
 *     ]
 *     logFolder = "${project.rootDir}".
 *   }
 *
 *
 * To enable affected module detection, you need to pass [ENABLE_ARG] into the build as a command line parameter
 * See [AffectedModuleDetector] for additional flags
 */
class AffectedModuleDetectorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        require(project.isRoot) {
            "Must be applied to root project, but was found on ${project.path} instead."
        }
        registerExtensions(project)
        registerAffectedAndroidTests(project)
        registerAffectedConnectedTestTask(project)
        registerJvmTests(project)

        project.gradle.projectsEvaluated {
            AffectedModuleDetector.configure(project.gradle, project)

            filterAndroidTests(project)
            filterJvmTests(project)
        }
    }

    private fun registerJvmTests(project: Project) {
        registerAffectedTestTask(TestType.JvmTest("runAffectedUnitTests", TASK_GROUP_NAME, "Runs all affected unit tests"), project)
    }

    private fun registerAffectedConnectedTestTask(rootProject: Project) {
        registerAffectedTestTask(TestType.RunAndroidTest("runAffectedAndroidTests", TASK_GROUP_NAME, "Runs all affected Android Tests. Requires a connected device. "), rootProject)
    }

    private fun registerAffectedAndroidTests(rootProject: Project) {
        registerAffectedTestTask(TestType.AssembleAndroidTest("assembleAffectedAndroidTests", TASK_GROUP_NAME, "Assembles all affected Android Tests.  Useful when working with device labs."), rootProject)
    }

    internal fun registerAffectedTestTask(
            testType: TestType,
            rootProject: Project
    ) {
        val task = rootProject.tasks.register(testType.name).get()
        task.group = testType.group
        task.description = testType.description

        rootProject.subprojects { project ->
            project.afterEvaluate {
                val pluginIds = setOf("com.android.application", "com.android.library", "java-library", "kotlin")
                pluginIds.forEach { pluginId ->
                    if (pluginId == "java-library" || pluginId == "kotlin") {
                        if (testType is TestType.JvmTest ) {
                            withPlugin(pluginId, task, testType, project)
                        }
                    } else {
                        withPlugin(pluginId, task, testType, project)
                    }
                }
            }

        }
    }

    private fun withPlugin(pluginId: String, task: Task, testType: TestType, project: Project) {
        project.pluginManager.withPlugin(pluginId) {
            val path = getAffectedPath(testType, project)
            path?.let {
                task.dependsOn(it)
                project.tasks.findByPath(it)?.onlyIf {
                    AffectedModuleDetector.isProjectAffected(project)
                }
            }

        }
    }

    private fun getAffectedPath(
        testType: TestType,
        project: Project
    ): String? {
        val tasks = requireNotNull(
            project.extensions.findByName(AffectedTestConfiguration.name)
        ) {
            "Unable to find ${AffectedTestConfiguration.name} in $project"
        } as AffectedTestConfiguration

        return when (testType) {
            is TestType.RunAndroidTest -> getPathAndTask(project, tasks.runAndroidTestTask)
            is TestType.AssembleAndroidTest -> getPathAndTask(project, tasks.assembleAndroidTestTask)
            is TestType.JvmTest -> getPathAndTask(project, tasks.jvmTestTask)
        }
    }

    private fun getPathAndTask(project: Project, task: String?): String? {
        return if (task.isNullOrEmpty()) {
            null
        } else {
            "${project.path}:${task}"
        }
    }

    private fun filterAndroidTests(project: Project) {
        val tracker = DependencyTracker(project, null)
        project.tasks.all { task ->
            if (task.name.contains("AndroidTest")) {
                tracker.findAllDependents(project).forEach { dependentProject ->
                    dependentProject.tasks.forEach { dependentTask ->
                        AffectedModuleDetector.configureTaskGuard(dependentTask)
                    }
                }
                AffectedModuleDetector.configureTaskGuard(task)
            }
        }
    }

    // Only allow unit tests to run if the AffectedModuleDetector says to include them
    private fun filterJvmTests(project: Project) {
        project.tasks.withType(Test::class.java) { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }
    }

    private fun registerExtensions(project: Project) {
        project.extensions.add(
            AffectedModuleConfiguration.name,
            AffectedModuleConfiguration()
        )
        project.subprojects { subproject ->
            subproject.extensions.add(
                AffectedTestConfiguration.name,
                AffectedTestConfiguration()
            )
        }
    }
    
    internal sealed class TestType(open val name: String, open val group: String, open val description: String) {
        data class RunAndroidTest(override val name: String, override val group: String, override val  description: String) : TestType(name, group, description)
        data class AssembleAndroidTest(override val name: String, override val group: String, override val  description: String) : TestType(name, group, description)
        data class JvmTest(override val name: String, override val group: String, override val  description: String) : TestType(name, group, description)
    }

    companion object {
        const val TASK_GROUP_NAME = "Affected Module Detector"
    }
}
