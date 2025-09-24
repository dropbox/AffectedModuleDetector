package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha

class SpecifiedBranchCommit(private val branch: String) : CommitShaProvider {

    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        return commandRunner.executeAndParseFirst("git rev-parse $branch")
    }
}
