package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

interface CommitShaProvider {
    fun getCommitSha(commandRunner: GitClient.CommandRunner): Sha?

    companion object {
        fun fromString(string: String): CommitShaProvider {
            return when (string) {
                "PreviousCommit" -> PreviousCommit()
                else -> ForkCommit()
            }
        }
    }
}

