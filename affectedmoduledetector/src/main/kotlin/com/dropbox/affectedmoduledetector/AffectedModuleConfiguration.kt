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

    companion object {
        const val name = "affectedModuleDetector"
    }
}