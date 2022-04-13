package com.dropbox.affectedmoduledetector

/**
 * Used to configure which variant to run for affected tasks by adding following block to modules
 * affectedTestConfiguration{
 *  assembleAndroidTestTask = "assembleDevDebugAndroidTest"
 * }
 */
open class AffectedTestConfiguration {

    var assembleAndroidTestTask : String? = "assembleDebugAndroidTest"
    var runAndroidTestTask : String?  = "connectedDebugAndroidTest"
    var jvmTestTask : String? = DEFAULT_JVM_TEST_TASK

    companion object {
        const val name = "affectedTestConfiguration"
        internal const val DEFAULT_JVM_TEST_TASK = "testDebugUnitTest"
    }
}