/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.affectedmoduledetector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test
import java.util.*

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
 * See [AffectedModuleDetector] for additonal flags
 */
class AffectedModuleDetectorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        require(project.isRoot) {
            "Must be applied to root project, but was found on ${project.path} instead."
        }
        registerExtensions(project)
        project.gradle.projectsEvaluated {
            AffectedModuleDetector.configure(project.gradle, project)

            registerAffectedAndroidTests(project)
            registerAffectedConnectedTestTask(project)
            registerJVMTests(project)

            filterAndroidTests(project)
            filterUnitTests(project)
        }
    }

    private fun registerJVMTests(project: Project) {
        registerAffectedTestTask(TestType.JvmTest("runAffectedUnitTests"), project)
    }

    private fun registerAffectedConnectedTestTask(rootProject: Project) {
        registerAffectedTestTask(TestType.RunAndroidTest("runAffectedAndroidTests"), rootProject)
    }

    private fun registerAffectedAndroidTests(rootProject: Project) {
        registerAffectedTestTask(TestType.AssembleAndroidTest("assembleAffectedAndroidTests"), rootProject)
    }

    private fun registerAffectedTestTask(
        testType: TestType,
        rootProject: Project
    ) {
        val task = rootProject.tasks.register(taskName).get()
        task.group = TASK_GROUP_NAME
        rootProject.subprojects { project ->
            val pluginIds = listOf("com.android.application", "com.android.library", "java-library")
                pluginIds.forEach { pluginId ->
                    if (pluginId == "java-library") {
                        if (testType is TestType.JvmTest ) {
                            withPlugin(pluginId, task, testType, project)
                        }
                    } else {
                        withPlugin(pluginId, task, testType, project)
                    }
                }           
            }
            task.enabled = paths.isNotEmpty()
            task.onlyIf { paths.isNotEmpty() }
        }
    }

    private fun withPlugin(pluginId: String, task: Task, testType: TestType, project: Project) {
        project.pluginManager.withPlugin(pluginId) {
            val path = getAffectedPath(testType, project)
            path?.let {
                task.dependsOn(it)
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

        val pathName = when (testType) {
            is TestType.RunAndroidTest -> "${project.path}:${tasks.runAndroidTestTask}"
            is TestType.AssembleAndroidTest -> "${project.path}:${tasks.assembleAndroidTestTask}"
            is TestType.JvmTest -> "${project.path}:${tasks.jvmTest}"
        }

        return if (AffectedModuleDetector.isProjectAffected(project)) {
            pathName
        } else {
            null
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
    private fun filterUnitTests(project: Project) {
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

    companion object {
        const val TASK_GROUP_NAME = "Affected Module Detector"
    }
    
    private sealed class TestType(open val name: String) {
        data class RunAndroidTest(override val name: String) : TestType(name)
        class AssembleAndroidTest(override val name: String) : TestType(name)
        class JvmTest(override val name: String) : TestType(name)
    }
}
