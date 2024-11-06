package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha
import java.io.Serializable

interface CommitShaProvider : Serializable {
    fun get(commandRunner: GitClient.CommandRunner): Sha
}

data class CommitShaProviderConfiguration(
    val type: String,
    val specifiedBranch: String? = null,
    val top: Sha,
    val includeUncommitted: Boolean
) : Serializable
