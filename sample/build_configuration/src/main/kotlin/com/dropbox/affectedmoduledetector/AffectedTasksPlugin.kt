package com.dropbox.affectedmoduledetector

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

var TEST_TASK_TO_RUN_EXTENSION = "TestTasks"

open class TestTasks {
    val assembleAndroidTestTask = "assembleDebugAndroidTest"
    val runAndroidTestTask = "connectedDebugAndroidTest"
    val jvmTest = "testDebugUnitTest"
    val jvmTestBackup = "test"
}

/**
 * unlike [AffectedTestsPlugin] which skips unaffected tests, this plugn instead creates a task and
 * registers all affected test tasks. Advantage is speed in not needing to skip modules at a large scale
 *
 * Registers 3 tasks
 * gradlew runCustomAffectedUnitTests - runs jvm tests
 * gradlew runCustomAffectedAndroidUnitTests - runs connected tests
 * gradlew customAssembleAffectedAndroidTests - assembles but does not run on device tests, useful when working with device labs
 */
class AffectedTasksPlugin : Plugin<Project> {
    var ANDROID_TEST_BUILD_VARIANT = "AndroidTest"

    lateinit var testTasks: TestTasks
    override fun apply(project: Project) {
        project.extensions.add(TEST_TASK_TO_RUN_EXTENSION, TestTasks())
        project.afterEvaluate {
            val rootProject = project.rootProject
            testTasks = requireNotNull(
                project.extensions.findByName(TEST_TASK_TO_RUN_EXTENSION)
            ) as TestTasks
            registerAffectedTestTask(
                "runCustomAffectedUnitTests",
                testTasks.jvmTest, testTasks.jvmTestBackup, rootProject,
            )

            registerAffectedAndroidTests(rootProject)
            registerAffectedConnectedTestTask(rootProject)
        }

        filterAndroidTests(project)
        filterUnitTests(project)
    }

    private fun registerAffectedTestTask(
        taskName: String, testTask: String, testTaskBackup: String?,
        rootProject: Project
    ) {
        rootProject.tasks.register(taskName) { task ->
            val paths = getAffectedPaths(testTask, testTaskBackup, rootProject)
            paths.forEach { path ->
                task.dependsOn(path)
            }
            task.enabled = paths.isNotEmpty()
            task.onlyIf { paths.isNotEmpty() }
        }
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

    private fun registerAffectedConnectedTestTask(
        rootProject: Project
    ) {
        registerAffectedTestTask(
            "runCustomAffectedAndroidUnitTests",
            testTasks.runAndroidTestTask, null, rootProject
        )
    }

    private fun registerAffectedAndroidTests(
        rootProject: Project
    ) {
        registerAffectedTestTask(
            "customAssembleAffectedAndroidTests",
            testTasks.assembleAndroidTestTask, null, rootProject
        )
    }


    private fun filterAndroidTests(project: Project) {
        val tracker = DependencyTracker(project, null)
        project.tasks.configureEach { task ->
            if (task.name.contains(ANDROID_TEST_BUILD_VARIANT)) {
                tracker.findAllDependents(project.projectPath).forEach { dependentProject ->
                    project.findProject(dependentProject.path)?.tasks?.forEach { dependentTask ->
                        AffectedModuleDetector.configureTaskGuard(dependentTask)
                    }
                }
                AffectedModuleDetector.configureTaskGuard(task)
            }
        }
    }

    private fun filterUnitTests(project: Project) {
        project.tasks.withType(Test::class.java) { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }
    }
}
