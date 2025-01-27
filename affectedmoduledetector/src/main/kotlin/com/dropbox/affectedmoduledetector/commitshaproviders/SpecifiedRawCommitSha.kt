package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

class SpecifiedRawCommitSha(private val commitSha: String) : CommitShaProvider {
    override fun get(commandRunner: GitClient.CommandRunner): Sha {
        return commitSha
    }
}
