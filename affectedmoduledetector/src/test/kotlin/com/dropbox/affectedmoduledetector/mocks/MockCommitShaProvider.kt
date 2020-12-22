package com.dropbox.affectedmoduledetector.mocks

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProvider

class MockCommitShaProvider: CommitShaProvider {
    private val replies = mutableListOf<Sha>()

    fun addReply(sha: Sha) {
        replies.add(sha)
    }
    override fun getCommitSha(commandRunner: GitClient.CommandRunner): Sha {
        return replies.first()
    }
}