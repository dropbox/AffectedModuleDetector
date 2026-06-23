package com.dropbox.affectedmoduledetector

import org.gradle.api.provider.Provider
import java.io.Serializable

/**
 * Used to configure which variant to run for affected tasks by adding following block to modules
 * affectedTestConfiguration{
 *  assembleAndroidTestTask = "assembleDevDebugAndroidTest"
 * }
 */
open class AffectedTestConfiguration {
    var testTasksProvider: Provider<TaskNames>? = null

    fun getTestTaskNames(): TaskNames {
        return testTasksProvider?.orNull ?: TaskNames()
    }

    data class TaskNames(
        val unitTestTasks: List<String> = listOf(DEFAULT_JVM_TEST_TASK),
        val androidTestTasks: List<String> = listOf(DEFAULT_ANDROID_TEST_TASK),
        val assembleAndroidTestTasks: List<String> = listOf(DEFAULT_ASSEMBLE_ANDROID_TEST_TASK),
    ): Serializable

    companion object {
        const val name = "affectedTestConfiguration"

        internal const val DEFAULT_JVM_TEST_TASK = "testDebugUnitTest"
        internal const val DEFAULT_NON_ANDROID_JVM_TEST_TASK = "test"
        internal const val DEFAULT_ASSEMBLE_ANDROID_TEST_TASK = "assembleDebugAndroidTest"
        internal const val DEFAULT_ANDROID_TEST_TASK = "connectedDebugAndroidTest"
    }
}
