package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.util.toOsSpecificPath
import java.io.File
import java.io.Serializable

class AffectedModuleConfiguration : Serializable {

    /**
     * Implementation of [AffectedModuleTaskType] for easy adding of custom gradle task to
     * AffectedModuleDetector. You can declare a new instance of it in build.gradle.
     *
     * @see AffectedModuleTaskType - interface
     * @see customTasks - configuration field
     */
    data class CustomTask(
        override val commandByImpact: String,
        override val originalGradleCommand: String,
        override val taskDescription: String
    ) : AffectedModuleTaskType

    /**
     * If you want to add a custom task for impact analysis you must set the list
     * of [AffectedModuleTaskType] implementations.
     *
     * Example:
     * `build.gradle
     *
     *  affectedModuleDetector {
     *       ...
     *       customTasks = [ // <- list of custom gradle invokes
     *           new AffectedModuleConfiguration.CustomTask(
     *                "runSomeCustomTaskByImpact",
     *                "someTaskForExample",
     *                "Task description."
     *            )
     *       ]
     *       ...
     *  }
     * `
     *
     * @see AffectedModuleTaskType - interface
     * @see CustomTask - Implementation class
     * @see AffectedModuleDetectorPlugin - gradle plugin
     */
    var customTasks = emptySet<CustomTask>()

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
            // Protect against users specifying the wrong path separator for their OS.
            field = value.map { it.toOsSpecificPath() }.toSet()
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

    var specifiedRawCommitSha: String? = null

    var compareFrom: String = "PreviousCommit"
        set(value) {
            val commitShaProviders = listOf(
                "PreviousCommit",
                "ForkCommit",
                "SpecifiedBranchCommit",
                "SpecifiedBranchCommitMergeBase",
                "SpecifiedRawCommitSha"
            )
            require(commitShaProviders.contains(value)) {
                "The property configuration compareFrom must be one of the following: ${commitShaProviders.joinToString(", ")}"
            }
            if (value == "SpecifiedBranchCommit" || value == "SpecifiedBranchCommitMergeBase") {
                requireNotNull(specifiedBranch) {
                    "Specify a branch using the configuration specifiedBranch"
                }
            }
            if (value == "SpecifiedRawCommitSha") {
                requireNotNull(specifiedRawCommitSha) {
                    "Provide a Commit SHA for the specifiedRawCommitSha property when using SpecifiedRawCommitSha comparison strategy."
                }
            }
            field = value
        }

    /**
     * A set of modules that will not be considered in the build process, even if changes are made in them.
     */
    var excludedModules = emptySet<String>()

    /**
     * A set of files that will be filtered out of the list of changed files retrieved by git.
     */
    var ignoredFiles = emptySet<String>()

    /**
     * If uncommitted files should be considered affected
     */
    var includeUncommitted: Boolean = true

    /**
     * If we should build all projects when no projects have changed
     */
    var buildAllWhenNoProjectsChanged: Boolean = true

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

    companion object {

        const val name = "affectedModuleDetector"
    }
}
