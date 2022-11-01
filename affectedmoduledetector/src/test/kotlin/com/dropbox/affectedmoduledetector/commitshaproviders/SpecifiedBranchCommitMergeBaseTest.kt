package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.AttachLogsTestRule
import com.dropbox.affectedmoduledetector.mocks.MockCommandRunner
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpecifiedBranchCommitMergeBaseTest {

    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    private val previousCommit = SpecifiedBranchCommitMergeBase(SPECIFIED_BRANCH)

    @Test
    fun `WHEN CURRENT_BRANCH_CMD THEN command returned`() {
        Truth.assertThat(SpecifiedBranchCommitMergeBase.CURRENT_BRANCH_CMD).isEqualTo("git rev-parse --abbrev-ref HEAD")
    }

    @Test
    fun `WHEN get commit sha THEN return sha`() {

        commandRunner.addReply(SpecifiedBranchCommitMergeBase.CURRENT_BRANCH_CMD, NEW_FEATURE_BRANCH)
        commandRunner.addReply("git merge-base $NEW_FEATURE_BRANCH $SPECIFIED_BRANCH", "commit-sha")

        val actual = previousCommit.get(commandRunner)

        Truth.assertThat(actual).isEqualTo("commit-sha")
    }

    private companion object {

        const val SPECIFIED_BRANCH = "origin/dev"
        const val NEW_FEATURE_BRANCH = "newFeatureBranch"
    }
}
