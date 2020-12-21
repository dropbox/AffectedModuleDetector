package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

class PreviousCommit: CommitShaProvider {
    override fun getCommitSha(commandRunner: GitClient.CommandRunner): Sha {
        return commandRunner.executeAndParseFirst(PREV_COMMIT_CMD)
    }
    companion object {
        const val PREV_COMMIT_CMD = "git --no-pager rev-parse HEAD~1"
    }
}