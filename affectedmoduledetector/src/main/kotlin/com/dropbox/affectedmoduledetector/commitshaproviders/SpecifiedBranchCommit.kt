package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

class SpecifiedBranchCommit(private val branch: String) : CommitShaProvider {

    override fun get(commandRunner: GitClient.CommandRunner): Sha {
        return commandRunner.executeAndParseFirst("git rev-parse $branch")
    }
}