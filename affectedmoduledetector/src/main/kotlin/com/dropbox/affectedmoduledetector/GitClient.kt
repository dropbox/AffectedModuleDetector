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

package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProvider
import java.io.File
import java.util.concurrent.TimeUnit
import org.gradle.api.logging.Logger

interface GitClient {
    fun findChangedFiles(
        top: Sha = "HEAD",
        includeUncommitted: Boolean = false
    ): List<String>
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
        /**
         * Executes the given shell command and returns the first stdout line.
         */
        fun executeAndParseFirst(command: String): String
    }
}

typealias Sha = String

/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
internal class GitClientImpl(
    /**
     * The root location for git
     */
    private val workingDir: File,
    private val logger: Logger?,
    private val commandRunner: GitClient.CommandRunner = RealCommandRunner(
        workingDir = workingDir,
        logger = logger
    ),
    private val commitShaProvider: CommitShaProvider
) : GitClient {

    /**
     * Finds changed file paths
     */
    override fun findChangedFiles(
        top: Sha,
        includeUncommitted: Boolean
    ): List<String> {
        val sha = commitShaProvider.get(commandRunner)

        // use this if we don't want local changes
        return commandRunner.executeAndParse(if (includeUncommitted) {
            "$CHANGED_FILES_CMD_PREFIX HEAD..$sha"
        } else {
            "$CHANGED_FILES_CMD_PREFIX $top $sha"
        })
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
        val commitLog: MutableList<Commit> = mutableListOf()
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

            val stdout = proc
                .inputStream
                .bufferedReader()
                .readText()
            val stderr = proc
                .errorStream
                .bufferedReader()
                .readText()

            proc.waitFor(5, TimeUnit.MINUTES)

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

        override fun executeAndParseFirst(command: String): String {
            return requireNotNull(
                executeAndParse(command)
                    .firstOrNull()
                    ?.split(" ")
                    ?.firstOrNull()
            ) {
                "No value from command: $command provided"
            }
        }
    }

    companion object {
        const val CHANGED_FILES_CMD_PREFIX = "git --no-pager diff --name-only"
    }
}

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