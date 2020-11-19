package com.dropbox.affectedmoduledetector

import java.io.File

/**
 * Used to configure which variant to run for affected tasks by adding following block to modules
 * affectedTestConfiguration{
 *  variantToTest = "debug"
 *  jvmTestBackup = "test"
 * }
 */
class AffectedTestConfiguration {

    /**
     *  Sets variant for all affected tasks
     *  By default `run` tasks will use debug as the variant to test ie
     *  gradlew runAffectedUnitTests will run testDebugUnitTest
     *
     */
    var variantToTest = "debug"

    /**
     * when [jvmTest] task is not found we will try to run [jvmTestBackup]
     * this is normally used for modules that are not android variant aware
     */
    var jvmTestBackup = "test"

    val assembleAndroidTestTask = "assemble${variantToTest.capitalize()}AndroidTest"
    val runAndroidTestTask = "connected${variantToTest.capitalize()}AndroidTest"
    val jvmTest = "test${variantToTest.capitalize()}UnitTest"

    companion object {
        const val name = "affectedTestConfiguration"
    }
}