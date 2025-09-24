package com.dropbox.affectedmoduledetector.vcs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.io.Serializable

interface VcsClient: Serializable {
    fun findChangedFiles(
        project: Project,
    ): Provider<List<String>>

    fun getVcsRoot(): File

    /**
     * Abstraction for running execution commands for testability
     */
    interface CommandRunner {
        /**
         * Executes the given shell command and returns the stdout as a string.
         */
        fun execute(command: String): String

        /**
         * Executes the given shell command and returns the stdout by lines.
         */
        fun executeAndParse(command: String): List<String>

        /**
         * Executes the given shell command and returns the first stdout line.
         */
        fun executeAndParseFirst(command: String): String
    }
}
