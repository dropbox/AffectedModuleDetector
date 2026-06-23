package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha

class SpecifiedBranchCommitMergeBase(private val specifiedBranch: String) : CommitShaProvider {

    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        val currentBranch = commandRunner.executeAndParseFirst(CURRENT_BRANCH_CMD)
        return commandRunner.executeAndParseFirst("git merge-base $currentBranch $specifiedBranch")
    }

    companion object {

        const val CURRENT_BRANCH_CMD = "git rev-parse --abbrev-ref HEAD"
    }
}
