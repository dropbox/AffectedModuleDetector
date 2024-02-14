package com.dropbox.affectedmoduledetector

/**
 * Used to configure which variant to run for affected tasks by adding following block to modules
 * affectedTestConfiguration{
 *  assembleAndroidTestTask = "assembleDevDebugAndroidTest"
 * }
 */
open class AffectedTestConfiguration {

    var assembleAndroidTestTask: String? = DEFAULT_ASSEMBLE_ANDROID_TEST_TASK
    var runAndroidTestTask: String? = DEFAULT_ANDROID_TEST_TASK
    var jvmTestTask: String? = DEFAULT_JVM_TEST_TASK

    companion object {
        const val name = "affectedTestConfiguration"

        internal const val DEFAULT_JVM_TEST_TASK = "testDebugUnitTest"
        internal const val DEFAULT_NON_ANDROID_JVM_TEST_TASK = "test"
        internal const val DEFAULT_ASSEMBLE_ANDROID_TEST_TASK = "assembleDebugAndroidTest"
        internal const val DEFAULT_ANDROID_TEST_TASK = "connectedDebugAndroidTest"
    }
}
