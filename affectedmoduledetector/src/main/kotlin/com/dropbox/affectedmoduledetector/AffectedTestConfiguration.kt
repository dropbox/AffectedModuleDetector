package com.dropbox.affectedmoduledetector

/**
 * Used to configure which variant to run for affected tasks by adding following block to modules
 * affectedTestConfiguration{
 *  assembleAndroidTestTask = "assembleDevDebugAndroidTest"
 * }
 */
open class AffectedTestConfiguration {

    var assembleAndroidTestTask = "assembleDebugAndroidTest"
    var runAndroidTestTask  = "connectedDebugAndroidTest"
    var jvmTest = "testDebugUnitTest"

    companion object {
        const val name = "affectedTestConfiguration"
    }
}