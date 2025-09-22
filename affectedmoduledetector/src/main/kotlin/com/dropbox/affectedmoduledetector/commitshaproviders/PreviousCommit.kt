package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha

class PreviousCommit : CommitShaProvider {
    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        return commandRunner.executeAndParseFirst(PREV_COMMIT_CMD)
    }
    companion object {
        const val PREV_COMMIT_CMD = "git --no-pager rev-parse HEAD~1"
    }
}
