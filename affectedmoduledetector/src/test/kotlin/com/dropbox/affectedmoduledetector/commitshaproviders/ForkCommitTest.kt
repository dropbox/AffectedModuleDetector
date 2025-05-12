package com.dropbox.affectedmoduledetector.commitshaproviders

import com.dropbox.affectedmoduledetector.AttachLogsTestRule
import com.dropbox.affectedmoduledetector.mocks.MockCommandRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ForkCommitTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    private val forkCommit = ForkCommit()

    @Test
    fun whenCURRENT_BRANCH_CMD_thenCommandReturned() {
        assertThat(ForkCommit.CURRENT_BRANCH_CMD).isEqualTo("git rev-parse --abbrev-ref HEAD")
    }

    @Test
    fun whenSHOW_ALL_BRANCHES_CMD_thenCommandReturned() {
        assertThat(ForkCommit.SHOW_ALL_BRANCHES_CMD).isEqualTo("git show-branch -a")
    }

    @Test
    fun givenNoParentBranchThatIsNotCurrentBranch_whenGetCommitSha_thenThrowException() {
        try {
            commandRunner.addReply(ForkCommit.CURRENT_BRANCH_CMD, "main")
            val parentBranches = listOf("[main]")
            commandRunner.addReply(
                ForkCommit.SHOW_ALL_BRANCHES_CMD,
                parentBranches.joinToString(System.lineSeparator())
            )

            forkCommit.get(commandRunner)
            fail()
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Parent branch not found")
        }
    }

    @Test
    fun givenNoParentBranchThatContainsAsterisk_whenGetCommitSha_thenThrowException() {
        try {
            commandRunner.addReply(ForkCommit.CURRENT_BRANCH_CMD, "feature")
            val parentBranches = listOf("[main]")
            commandRunner.addReply(
                ForkCommit.SHOW_ALL_BRANCHES_CMD,
                parentBranches.joinToString(System.lineSeparator())
            )

            forkCommit.get(commandRunner)
            fail()
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Parent branch not found")
        }
    }

    @Test
    fun givenParentBranchWithSymbols_whenGetCommitSha_thenReturnForkCommitSha() {
        commandRunner.addReply(ForkCommit.CURRENT_BRANCH_CMD, "feature")
        val parentBranches = listOf("* [main^~SNAPSHOT_01]")
        commandRunner.addReply(ForkCommit.SHOW_ALL_BRANCHES_CMD, parentBranches.joinToString(System.lineSeparator()))
        commandRunner.addReply("git merge-base feature main", "commit-sha")

        val actual = forkCommit.get(commandRunner)

        assertThat(actual).isEqualTo("commit-sha")
    }

    @Test
    fun givenProvidedParentBranch_whenGetCommitSha_thenUseProvidedBranch() {
        val providedParentBranch = "main"
        val forkCommitWithParent = ForkCommit(providedParentBranch)
        
        commandRunner.addReply(ForkCommit.CURRENT_BRANCH_CMD, "feature")
        commandRunner.addReply("git merge-base feature main", "commit-sha")

        val actual = forkCommitWithParent.get(commandRunner)

        assertThat(actual).isEqualTo("commit-sha")
    }
}
