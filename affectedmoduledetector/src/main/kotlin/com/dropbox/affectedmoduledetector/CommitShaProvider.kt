package com.dropbox.affectedmoduledetector

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

class PreviousCommit: CommitShaProvider {
    override fun getCommitSha(commandRunner: GitClient.CommandRunner): Sha? {
        return commandRunner.executeAndParse(PREV_COMMIT_CMD)
            .firstOrNull()
            ?.split(" ")
            ?.firstOrNull()
    }
    companion object {
        const val PREV_COMMIT_CMD = "git --no-pager rev-parse HEAD~1"
    }
}

class ForkCommit: CommitShaProvider {
    override fun getCommitSha(commandRunner: GitClient.CommandRunner): Sha? {
        val currentBranch = commandRunner.executeAndParse(CURRENT_BRANCH_CMD).firstOrNull()

        requireNotNull(currentBranch) {
            "Current branch not found"
        }

        val parentBranch = commandRunner.executeAndParse(SHOW_ALL_BRANCHES_CMD)
            .filter { it.contains("*") }
            .first { !it.contains(currentBranch) }
            .substringAfter("[")
            .substringBefore("]")
            .substringBefore("~")
            .substringBefore("^")

        require(parentBranch.isNotEmpty()) {
            "Parent branch not found"
        }

        return commandRunner.executeAndParse("git merge-base $currentBranch $parentBranch")
            .firstOrNull()
            ?.split(" ")
            ?.firstOrNull()
    }

    companion object {
        const val CURRENT_BRANCH_CMD = "git rev-parse --abbrev-ref HEAD"
        const val SHOW_ALL_BRANCHES_CMD = "git show-branch -a"
    }
}