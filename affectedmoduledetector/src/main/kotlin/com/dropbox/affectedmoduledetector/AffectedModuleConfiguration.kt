package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.plugin.AffectedModuleTaskType
import java.io.File

class AffectedModuleConfiguration {

    companion object {

        const val name = "affectedModuleDetector"
    }

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

    var specifiedBranch: String? = null

    var compareFrom: String = "PreviousCommit"
    set(value) {
        val commitShaProviders = listOf("PreviousCommit", "ForkCommit", "SpecifiedBranchCommit")
        require(commitShaProviders.contains(value)) {
            "The property configuration compareFrom must be one of the following: ${commitShaProviders.joinToString(", ")}"
        }
        if (value == "SpecifiedBranchCommit") {
            requireNotNull(specifiedBranch) {
                "Specify a branch using the configuration specifiedBranch"
            }
        }
        field = value
    }

    /**
     * A set of modules that will not be considered in the build process, even if changes are made in them.
     */
    var excludedModules = emptySet<String>()

    /**
     * If uncommitted files should be considered affected
     */
    var includeUncommitted: Boolean = true

    /**
     * If you want to add a custom task for impact analysis you must set the list of enum's fields
     *
     * Example:
     * `build.gradle
     *
     *  affectedModuleDetector {
     *       baseDir = "${project.rootDir}"
     *       pathsAffectingAllModules = ["buildSrc/"]
     *       specifiedBranch = "dev"
     *       customTasks = [MyCustomTask.DETEKT_TASK] // <- list of enum fields
     *       compareFrom = "SpecifiedBranchCommit"
     *       includeUncommitted = false
     *  }
     * `
     *
     * @see AffectedModuleTaskType
     * @see AffectedModuleDetectorPlugin
     */
    var customTasks = emptySet<AffectedModuleTaskType>()

    /**
     * The top of the git log to use, only used when [includeUncommitted] is false
     */
    var top: String = "HEAD"
        set(value) {
            require(!includeUncommitted) {
                "Set includeUncommitted to false to set a custom top"
            }
            field = value
        }
}