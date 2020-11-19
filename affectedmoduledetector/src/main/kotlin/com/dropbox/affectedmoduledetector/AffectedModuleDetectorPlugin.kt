/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.affectedmoduledetector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test
import java.util.LinkedHashSet
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
 *   baseDir = "${project.rootDir}"
 *   pathsAffectingAllModules = [
 *       "buildSrc/"
 *   ]
 *   variantToTest = "debug"
 *   logFolder = "${project.rootDir}".
 *   }
 *
 *
 * To enable affected module detection, you need to pass [ENABLE_ARG] into the build as a command line parameter
 * See [AffectedModuleDetector] for additonal flags
 */
class AffectedModuleDetectorPlugin : Plugin<Project> {

    lateinit var testTasks: AffectedModuleConfiguration

    override fun apply(project: Project) {
        require(project.isRoot) {
            "Must be applied to root project, but was found on ${project.path} instead."
        }
        project.extensions.add(
                AffectedModuleConfiguration.name,
                AffectedModuleConfiguration()
        )
        AffectedModuleDetector.configure(
                project.gradle,
                project
        )


        project.afterEvaluate {
            val rootProject = registerJVMTests(project)

            registerAffectedAndroidTests(rootProject)
            registerAffectedConnectedTestTask(rootProject)
        }

        filterAndroidTests(project)
        filterUnitTests(project)
    }

    private fun registerJVMTests(project: Project): Project {
        val rootProject = project.rootProject
        testTasks = requireNotNull(
                project.extensions.findByName(AffectedModuleConfiguration.name)
        ) as AffectedModuleConfiguration
        registerAffectedTestTask(
                "runAffectedUnitTests",
                testTasks.jvmTest, testTasks.jvmTestBackup, rootProject,
        )
        return rootProject
    }

    private fun registerAffectedConnectedTestTask(rootProject: Project) {
        registerAffectedTestTask(
                "runAffectedAndroidTests",
                testTasks.runAndroidTestTask, null, rootProject
        )
    }

    private fun registerAffectedAndroidTests(rootProject: Project) {
        registerAffectedTestTask(
                "buildTestApks",
                testTasks.assembleAndroidTestTask, null, rootProject
        )
    }

    private fun registerAffectedTestTask(
            taskName: String, testTask: String, testTaskBackup: String?,
            rootProject: Project): Task {
        val task = rootProject.tasks.register(taskName) { task ->
            val paths = getAffectedPaths(testTask, testTaskBackup, rootProject)
            paths.forEach { path ->
                task.dependsOn(path)
            }
            task.enabled = paths.isNotEmpty()
            task.onlyIf { paths.isNotEmpty() }
        }
        return task.get()
    }

    private fun getAffectedPaths(
            task: String,
            taskBackup: String?,
            rootProject: Project
    ):
            Set<String> {
        val paths = LinkedHashSet<String>()
        rootProject.subprojects { subproject ->
            val pathName = "${subproject.path}:$task"
            val backupPath = "${subproject.path}:$taskBackup"
            if (AffectedModuleDetector.isProjectProvided(subproject)) {
                if (subproject.tasks.findByPath(pathName) != null) {
                    paths.add(pathName)
                } else if (taskBackup != null &&
                        subproject.tasks.findByPath(backupPath) != null
                ) {
                    paths.add(backupPath)
                }
            }
        }
        return paths
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
}
