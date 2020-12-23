package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

interface CommitShaProvider {
    fun get(commandRunner: GitClient.CommandRunner): Sha

    companion object {
        fun fromString(string: String): CommitShaProvider {
            return when (string) {
                "PreviousCommit" -> PreviousCommit()
                "ForkCommit" -> ForkCommit()
                else -> throw IllegalArgumentException("Unsupported compareFrom type")
            }
        }
    }
}

