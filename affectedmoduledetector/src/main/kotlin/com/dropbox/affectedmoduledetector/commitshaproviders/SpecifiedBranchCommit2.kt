package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

class SpecifiedBranchCommit2(private val specifiedBranch: String) : CommitShaProvider {

    override fun get(commandRunner: GitClient.CommandRunner): Sha {
        val currentBranch = commandRunner.executeAndParseFirst(CURRENT_BRANCH_CMD)
        return commandRunner.executeAndParseFirst("git merge-base $currentBranch $specifiedBranch")
    }

    companion object {

        const val CURRENT_BRANCH_CMD = "git rev-parse --abbrev-ref HEAD"
    }
}
