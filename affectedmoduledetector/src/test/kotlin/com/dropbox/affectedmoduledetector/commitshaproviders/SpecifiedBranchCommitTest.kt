package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.AttachLogsTestRule
import com.dropbox.affectedmoduledetector.mocks.MockCommandRunner
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpecifiedBranchCommitTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    private val branch = "mybranch"
    private val previousCommit = SpecifiedBranchCommit(branch)

    @Test
    fun whenGetCommitSha_thenReturnCommitSha() {
        commandRunner.addReply("git rev-parse $branch", "commit-sha")

        val actual = previousCommit.get(commandRunner)

        Truth.assertThat(actual).isEqualTo("commit-sha")
    }
}