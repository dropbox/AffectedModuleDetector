package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.GitClient
import com.dropbox.affectedmoduledetector.Sha

interface CommitShaProvider {

    fun get(commandRunner: GitClient.CommandRunner): Sha

    companion object {
        fun fromString(
            string: String,
            specifiedBranch: String? = null,
            specifiedRawCommitSha: String? = null
        ): CommitShaProvider {
            return when (string) {
                "PreviousCommit" -> PreviousCommit()
                "ForkCommit" -> ForkCommit()
                "SpecifiedBranchCommit" -> {
                    requireNotNull(specifiedBranch) {
                        "Specified branch must be defined"
                    }
                    SpecifiedBranchCommit(specifiedBranch)
                }
                "SpecifiedBranchCommitMergeBase" -> {
                    requireNotNull(specifiedBranch) {
                        "Specified branch must be defined"
                    }
                    SpecifiedBranchCommitMergeBase(specifiedBranch)
                }
                "SpecifiedRawCommitSha" -> {
                    requireNotNull(specifiedRawCommitSha) {
                        "Specified raw commit sha must be defined"
                    }
                    SpecifiedRawCommitSha(specifiedRawCommitSha)
                }
                else -> throw IllegalArgumentException("Unsupported compareFrom type")
            }
        }
    }
}
