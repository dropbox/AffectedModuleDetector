package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha

class ForkCommit : CommitShaProvider {
    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        val currentBranch = commandRunner.executeAndParseFirst(CURRENT_BRANCH_CMD)

        val parentBranch = commandRunner.executeAndParse(SHOW_ALL_BRANCHES_CMD)
            .firstOrNull { !it.contains(currentBranch) && it.contains("*") }
            ?.substringAfter("[")
            ?.substringBefore("]")
            ?.substringBefore("~")
            ?.substringBefore("^")

        requireNotNull(parentBranch) {
            "Parent branch not found"
        }

        return commandRunner.executeAndParseFirst("git merge-base $currentBranch $parentBranch")
    }

    companion object {
        const val CURRENT_BRANCH_CMD = "git rev-parse --abbrev-ref HEAD"
        const val SHOW_ALL_BRANCHES_CMD = "git show-branch -a"
    }
}
