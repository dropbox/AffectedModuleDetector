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

package com.dropbox.affectedmoduledetector.vcs

import com.dropbox.affectedmoduledetector.FileLogger
import com.dropbox.affectedmoduledetector.vcs.GitClientImpl.Companion.CHANGED_FILES_CMD_PREFIX
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProviderConfiguration
import com.dropbox.affectedmoduledetector.util.toOsSpecificLineEnding
import com.dropbox.affectedmoduledetector.util.toOsSpecificPath
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.util.concurrent.TimeUnit


/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
@Suppress("UnstableApiUsage")
internal class GitClientImpl(
    /**
     * The root location for git
     */
    workingDir: File,
    logger: FileLogger?,
    commitShaProviderConfiguration: CommitShaProviderConfiguration,
    ignoredFiles: Set<String>?,
) : BaseVcsClient(
    workingDir = workingDir,
    logger = logger,
    commitShaProviderConfiguration = commitShaProviderConfiguration,
    ignoredFiles = ignoredFiles,
) {

    /**
     * Finds changed file paths
     */
    override fun findChangedFiles(
        project: Project,
    ): Provider<List<String>> {
        return project.providers.of(GitChangedFilesSource::class.java) {
            it.parameters.commitShaProviderConfiguration = commitShaProviderConfiguration
            it.parameters.workingDir.set(workingDir)
            it.parameters.logger = logger
            it.parameters.ignoredFiles.set(ignoredFiles)
        }
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

    override fun getVcsRoot(): File {
        return findGitDirInParentFilepath(workingDir) ?: workingDir
    }

    companion object {
        // -M95 is necessary to detect certain file moves.  See https://github.com/dropbox/AffectedModuleDetector/issues/60
        const val CHANGED_FILES_CMD_PREFIX = "git --no-pager diff --name-only -M95"
    }
}

private class RealCommandRunner(
    private val workingDir: File,
    private val logger: Logger?
) : VcsClient.CommandRunner {
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
        return execute(command).toOsSpecificLineEnding()
            .split(System.lineSeparator())
            .filterNot { it.isEmpty() }
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

/** Provides changed files since the last merge by calling git in [Parameters.workingDir]. */
@Suppress("UnstableApiUsage")
internal abstract class GitChangedFilesSource :
    ValueSource<List<String>, GitChangedFilesSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        var commitShaProviderConfiguration: CommitShaProviderConfiguration
        val workingDir: DirectoryProperty
        var logger: FileLogger?
        val ignoredFiles: SetProperty<String>
    }

    private val gitRoot by lazy {
        findGitDirInParentFilepath(parameters.workingDir.get().asFile)
    }

    private val commandRunner: VcsClient.CommandRunner by lazy {
        RealCommandRunner(
            workingDir = gitRoot ?: parameters.workingDir.get().asFile,
            logger = null
        )
    }

    override fun obtain(): List<String> {
        val top = parameters.commitShaProviderConfiguration.top
        val sha = getSha()

        // use this if we don't want local changes
        val changedFiles = commandRunner.executeAndParse(
            if (parameters.commitShaProviderConfiguration.includeUncommitted) {
                "$CHANGED_FILES_CMD_PREFIX $sha"
            } else {
                "$CHANGED_FILES_CMD_PREFIX $top..$sha"
            }
        ).map { it.toOsSpecificPath() }

        return parameters.ignoredFiles.orNull
            .orEmpty()
            .map { it.toRegex() }
            .foldRight(changedFiles) { ignoredFileRegex: Regex, fileList: List<String> ->
                fileList.filterNot { it.matches(ignoredFileRegex) }
            }
            .filterNot { it.isEmpty() }
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

    private fun getSha(): Sha {
        return parameters.commitShaProviderConfiguration.provider.get(commandRunner)
    }
}
