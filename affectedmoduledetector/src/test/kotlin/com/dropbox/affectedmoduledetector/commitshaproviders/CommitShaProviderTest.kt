package com.dropbox.affectedmoduledetector.commitshaproviders

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalArgumentException

@RunWith(JUnit4::class)
class CommitShaProviderTest {
    @Test
    fun givenPreviousCommit_whenFromString_thenReturnPreviousCommit() {
        val actual = CommitShaProvider.fromString("PreviousCommit")

        assertThat(actual::class).isEqualTo(PreviousCommit::class)
    }

    @Test
    fun givenForkCommit_whenFromString_thenReturnForkCommit() {
        val actual = CommitShaProvider.fromString("ForkCommit")

        assertThat(actual::class).isEqualTo(ForkCommit::class)
    }

    @Test
    fun givenSpecifiedBranchCommit_whenFromString_thenReturnSpecifiedBranchCommit() {
        val actual = CommitShaProvider.fromString("SpecifiedBranchCommit", "branch")

        assertThat(actual::class).isEqualTo(SpecifiedBranchCommit::class)
    }

    @Test
    fun givenSpecifiedBranchCommitAndSpecifiedBranchNull_whenFromString_thenReturnSpecifiedBranchCommit() {
        try {
            CommitShaProvider.fromString("SpecifiedBranchCommit")
            fail()
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Specified branch must be defined")
        }
    }

    @Test
    fun givenSpecifiedBranchCommitMergeBaseAndSpecifiedBranchNull_whenFromString_thenThrowException() {
        try {
            CommitShaProvider.fromString("SpecifiedBranchCommitMergeBase")
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Specified branch must be defined")
        }
    }

    @Test
    fun givenSpecifiedBranchCommitMergeBase_whenFromString_thenReturnSpecifiedBranchCommitMergeBase() {
        val actual = CommitShaProvider.fromString("SpecifiedBranchCommitMergeBase", "branch")

        assertThat(actual::class).isEqualTo(SpecifiedBranchCommitMergeBase::class)
    }

    @Test
    fun givenSpecifiedRawCommitSha_whenFromString_thenReturnSpecifiedRawCommitSha() {
        val actual =
            CommitShaProvider.fromString("SpecifiedRawCommitSha", specifiedRawCommitSha = "sha")

        assertThat(actual::class).isEqualTo(SpecifiedRawCommitSha::class)
    }

    @Test
    fun givenSpecifiedRawCommitShaAndSpecifiedRawCommitShaNull_whenFromString_thenThrowException() {
        try {
            CommitShaProvider.fromString("SpecifiedRawCommitSha")
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Specified raw commit sha must be defined")
        }
    }

    @Test
    fun givenInvalidCommitString_whenFromString_thenExceptionThrown() {
        try {
            CommitShaProvider.fromString("Invalid")
            fail()
        } catch (e: Exception) {
            assertThat(e::class).isEqualTo(IllegalArgumentException::class)
            assertThat(e.message).isEqualTo("Unsupported compareFrom type")
        }
    }
}
