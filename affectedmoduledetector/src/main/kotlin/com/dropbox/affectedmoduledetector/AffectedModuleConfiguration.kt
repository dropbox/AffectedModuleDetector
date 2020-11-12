package com.dropbox.affectedmoduledetector

import java.io.File

class AffectedModuleConfiguration {
    /**
     * Folder to place the log in
     */
    var logFolder: String? = null

    /**
     * Name for the log file
     */
    var logFilename: String = "affected_module_detector.log"

    /**
     * Base directory to use for [pathsAffectingAllModules]
     */
    var baseDir: String? = null
    /**
     * Files or folders which if changed will trigger all projects to be considered affected
     */
    var pathsAffectingAllModules = setOf<String>()
        set(value) {
            requireNotNull(baseDir) {
                "baseDir must be set to use pathsAffectingAllModules"
            }
            field = value
        }
        get() {
            field.forEach { path ->
                require(File(baseDir, path).exists()) {
                    "Could not find expected path in pathsAffectingAllModules: $path"
                }
            }
            return field
        }

    /**
     *  Sets variant for all affected tasks
     *  By default `run` tasks will use debug as the variant to test ie
     *  gradlew runAffectedUnitTests will run testDebugUnitTest
     *
     */
    var variantToTest = "debug"
    val assembleAndroidTestTask = "assemble${variantToTest.capitalize()}AndroidTest"
    val runAndroidTestTask = "connected${variantToTest.capitalize()}AndroidTest"
    val jvmTest = "test${variantToTest.capitalize()}UnitTest"

    /**
     * when [jvmTest] task is not found we will try to run [jvmTestBackup]
     * this is normally used for modules that are not android variant aware
     */
    var jvmTestBackup = "test"

    companion object {
        const val name = "affectedModuleDetector"
    }
}