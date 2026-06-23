package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha
import java.io.Serializable

interface CommitShaProvider : Serializable {
    fun get(commandRunner: VcsClient.CommandRunner): Sha
}

data class CommitShaProviderConfiguration(
    val provider: CommitShaProvider,
    val top: Sha,
    val includeUncommitted: Boolean
) : Serializable
