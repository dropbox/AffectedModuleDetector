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
    var jvmTestTask : String? = "testDebugUnitTest"

    companion object {
        const val name = "affectedTestConfiguration"
    }
}