package com.dropbox.affectedmoduledetector.commitshaproviders

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CommitShaProviderTest {
    @Test
    fun givenPreviousCommit_whenFromString_thenReturnPreviousCommit() {
        val actual = CommitShaProvider.fromString("PreviousCommit")

        assertThat(actual::class).isEqualTo(PreviousCommit::class)
    }

    @Test
    fun givenForkCommit_whenFromString_thenReturnPreviousCommit() {
        val actual = CommitShaProvider.fromString("ForkCommit")

        assertThat(actual::class).isEqualTo(ForkCommit::class)
    }
}