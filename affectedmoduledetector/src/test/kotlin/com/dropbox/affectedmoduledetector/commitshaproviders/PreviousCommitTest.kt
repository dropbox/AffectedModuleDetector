package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.AttachLogsTestRule
import com.dropbox.affectedmoduledetector.mocks.MockCommandRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PreviousCommitTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    private val previousCommit = PreviousCommit()

    @Test
    fun whenPREV_COMMIT_CMD_thenCommandReturned() {
        assertThat(PreviousCommit.PREV_COMMIT_CMD).isEqualTo("git --no-pager rev-parse HEAD~1")
    }

    @Test
    fun whenGetCommitSha_thenReturnCommitSha() {
        commandRunner.addReply(PreviousCommit.PREV_COMMIT_CMD, "commit-sha")

        val actual = previousCommit.get(commandRunner)

        assertThat(actual).isEqualTo("commit-sha")
    }
}
