package com.dropbox.affectedmoduledetector.vcs

import com.dropbox.affectedmoduledetector.FileLogger
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProviderConfiguration
import java.io.File
import java.io.Serializable

typealias Sha = String

abstract class BaseVcsClient(
    protected val workingDir: File,
    protected val logger: FileLogger?,
    protected val commitShaProviderConfiguration: CommitShaProviderConfiguration,
    protected val ignoredFiles: Set<String>?,
): VcsClient

abstract class BaseVcsClientProvider: Serializable {
    abstract fun get(
        workingDir: File,
        logger: FileLogger?,
        commitShaProviderConfiguration: CommitShaProviderConfiguration,
        ignoredFiles: Set<String>?
    ): BaseVcsClient
}
