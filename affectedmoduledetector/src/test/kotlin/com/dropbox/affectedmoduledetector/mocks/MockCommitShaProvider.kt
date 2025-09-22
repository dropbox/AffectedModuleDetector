package com.dropbox.affectedmoduledetector.mocks

import com.dropbox.affectedmoduledetector.vcs.VcsClient
import com.dropbox.affectedmoduledetector.vcs.Sha
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProvider

class MockCommitShaProvider : CommitShaProvider {
    private val replies = mutableListOf<Sha>()

    fun addReply(sha: Sha) {
        replies.add(sha)
    }
    override fun get(commandRunner: VcsClient.CommandRunner): Sha {
        return replies.first()
    }
}
