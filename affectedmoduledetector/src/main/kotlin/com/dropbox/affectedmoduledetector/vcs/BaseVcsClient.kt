package com.dropbox.affectedmoduledetector.vcs

import com.dropbox.affectedmoduledetector.FileLogger
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProviderConfiguration
import java.io.File

typealias Sha = String

abstract class BaseVcsClient(
    protected val workingDir: File,
    protected val logger: FileLogger?,
    protected val commitShaProviderConfiguration: CommitShaProviderConfiguration,
    protected val ignoredFiles: Set<String>?,
): VcsClient
