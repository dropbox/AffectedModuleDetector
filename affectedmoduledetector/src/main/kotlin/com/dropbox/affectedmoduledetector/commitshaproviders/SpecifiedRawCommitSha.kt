package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha

class SpecifiedRawCommitSha(private val commitSha: String) : CommitShaProvider {
    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        return commitSha
    }
}
