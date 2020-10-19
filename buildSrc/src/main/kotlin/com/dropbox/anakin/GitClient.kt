/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.anakin

import java.io.File
import java.util.concurrent.TimeUnit
import org.gradle.api.logging.Logger

interface GitClient {
    fun findChangedFilesSince(
        sha: String,
        top: String = "HEAD",
        includeUncommitted: Boolean = false
    ): List<String>
    fun findPreviousMergeCL(): String?

    fun getGitLog(
        gitCommitRange: GitCommitRange,
        keepMerges: Boolean,
        fullProjectDir: File
    ): List<Commit>

    fun getGitRoot(): File

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
    }
}
/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
class GitClientImpl(
    /**
     * The root location for git
     */
    private val workingDir: File,
    private val logger: Logger?,
    private val commandRunner: GitClient.CommandRunner = RealCommandRunner(
        workingDir = workingDir,
        logger = logger
    )
) : GitClient {

    /**
     * Finds changed file paths since the given sha
     */
    override fun findChangedFilesSince(
        sha: String,
        top: String,
        includeUncommitted: Boolean
    ): List<String> {
        // use this if we don't want local changes
        return commandRunner.executeAndParse(if (includeUncommitted) {
            "$CHANGED_FILES_CMD_PREFIX HEAD..$sha"
        } else {
            "$CHANGED_FILES_CMD_PREFIX $top $sha"
        })
    }

    /**
     * checks the history to find the first merge CL.
     */
    override fun findPreviousMergeCL(): String? {
        return commandRunner.executeAndParse(PREV_MERGE_CMD)
                .firstOrNull()
                ?.split(" ")
                ?.firstOrNull()
    }

    private fun findGitDirInParentFilepath(filepath: File): File? {
        var curDirectory: File = filepath
        while (curDirectory.path != "/") {
            if (File("$curDirectory/.git").exists()) {
                return curDirectory
            }
            curDirectory = curDirectory.parentFile
        }
        return null
    }
    @Suppress("LongParameterList")
    private fun parseCommitLogString(
        commitLogString: String,
        commitStartDelimiter: String,
        commitSHADelimiter: String,
        subjectDelimiter: String,
        authorEmailDelimiter: String,
        localProjectDir: String
    ): List<Commit> {
        // Split commits string out into individual commits (note: this removes the deliminter)
        val gitLogStringList: List<String>? = commitLogString.split(commitStartDelimiter)
        var commitLog: MutableList<Commit> = mutableListOf()
        gitLogStringList?.filter { gitCommit ->
            gitCommit.trim() != ""
        }?.forEach { gitCommit ->
            commitLog.add(
                Commit(
                    gitCommit,
                    localProjectDir,
                    commitSHADelimiter = commitSHADelimiter,
                    subjectDelimiter = subjectDelimiter,
                    authorEmailDelimiter = authorEmailDelimiter
                )
            )
        }
        return commitLog.toList()
    }

    /**
     * Converts a diff log command into a [List<Commit>]
     *
     * @param gitCommitRange the [GitCommitRange] that defines the parameters of the git log command
     * @param keepMerges boolean for whether or not to add merges to the return [List<Commit>].
     * @param fullProjectDir a [File] object that represents the full project directory.
     */
    override fun getGitLog(
        gitCommitRange: GitCommitRange,
        keepMerges: Boolean,
        fullProjectDir: File
    ): List<Commit> {
        val commitStartDelimiter: String = "_CommitStart"
        val commitSHADelimiter: String = "_CommitSHA:"
        val subjectDelimiter: String = "_Subject:"
        val authorEmailDelimiter: String = "_Author:"
        val dateDelimiter: String = "_Date:"
        val bodyDelimiter: String = "_Body:"
        val localProjectDir: String = fullProjectDir.relativeTo(getGitRoot()).toString()
        val relativeProjectDir: String = fullProjectDir.relativeTo(workingDir).toString()

        var gitLogOptions: String =
            "--pretty=format:$commitStartDelimiter%n" +
                    "$commitSHADelimiter%H%n" +
                    "$authorEmailDelimiter%ae%n" +
                    "$dateDelimiter%ad%n" +
                    "$subjectDelimiter%s%n" +
                    "$bodyDelimiter%b" +
                    if (!keepMerges) {
                        " --no-merges"
                    } else {
                        ""
                    }
        var gitLogCmd: String
        if (gitCommitRange.fromExclusive != "") {
            gitLogCmd = "$GIT_LOG_CMD_PREFIX $gitLogOptions " +
                    "${gitCommitRange.fromExclusive}..${gitCommitRange.untilInclusive}" +
                    " -- ./$relativeProjectDir"
        } else {
            gitLogCmd = "$GIT_LOG_CMD_PREFIX $gitLogOptions ${gitCommitRange.untilInclusive} -n " +
                    "${gitCommitRange.n} -- ./$relativeProjectDir"
        }
        val gitLogString: String = commandRunner.execute(gitLogCmd)
        val commits = parseCommitLogString(
            gitLogString,
            commitStartDelimiter,
            commitSHADelimiter,
            subjectDelimiter,
            authorEmailDelimiter,
            localProjectDir
        )
        if (commits.isEmpty()) {
            // Probably an error; log this
            logger?.warn("No git commits found! Ran this command: '" +
                    gitLogCmd + "' and received this output: '" + gitLogString + "'")
        }
        return commits
    }

    override fun getGitRoot(): File {
        return findGitDirInParentFilepath(workingDir) ?: workingDir
    }

    private class RealCommandRunner(
        private val workingDir: File,
        private val logger: Logger?
    ) : GitClient.CommandRunner {
        override fun execute(command: String): String {
            val parts = command.split("\\s".toRegex())
            logger?.info("running command $command in $workingDir")
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(5, TimeUnit.MINUTES)
            val stdout = proc
                .inputStream
                .bufferedReader()
                .readText()
            val stderr = proc
                .errorStream
                .bufferedReader()
                .readText()
            val message = stdout + stderr
            if (stderr != "") {
                logger?.error("Response: $message")
            } else {
                logger?.info("Response: $message")
            }
            check(proc.exitValue() == 0) { "Nonzero exit value running git command." }
            return stdout
        }
        override fun executeAndParse(command: String): List<String> {
            val response = execute(command)
                .split(System.lineSeparator())
                .filterNot {
                    it.isEmpty()
                }
            return response
        }
    }

    companion object {
        const val PREV_MERGE_CMD = "git --no-pager rev-parse HEAD~1"
        const val CHANGED_FILES_CMD_PREFIX = "git --no-pager diff --name-only"
        const val GIT_LOG_CMD_PREFIX = "git --no-pager log --name-only"
    }
}

/**
 * Defines the parameters for a git log command
 *
 * @property fromExclusive the oldest SHA at which the git log starts. Set to an empty string to use
 * [n]
 * @property untilInclusive the latest SHA included in the git log.  Defaults to HEAD
 * @property n a count of how many commits to go back to.  Only used when [fromExclusive] is an
 * empty string
 */
data class GitCommitRange(
    val fromExclusive: String = "",
    val untilInclusive: String = "HEAD",
    val n: Int = 0
)

/**
 * Class implementation of a git commit.  It uses the input delimiters to parse the commit
 *
 * @property gitCommit a string representation of a git commit
 * @property projectDir the project directory for which to parse file paths from a commit
 * @property commitSHADelimiter the term to use to search for the commit SHA
 * @property subjectDelimiter the term to use to search for the subject (aka commit summary)
 *           message
 * @property authorEmailDelimiter the term to use to search for the author email
 */
data class Commit(
    val gitCommit: String,
    val projectDir: String,
    private val commitSHADelimiter: String = "_CommitSHA:",
    private val subjectDelimiter: String = "_Subject:",
    private val authorEmailDelimiter: String = "_Author:"
)
