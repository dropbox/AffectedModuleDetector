package com.dropbox.affectedmoduledetector

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalTaskTypeTest {

    @Test
    fun `GIVEN InternalTaskType WHEN JVM_TEST WHEN THEN originalGradleCommand is "test"`() {
        // GIVEN
        val nonAndroidTestTask = InternalTaskType.JVM_TEST.originalGradleCommand
        val nonAndroidTestCommand = "test"

        assert(nonAndroidTestTask == nonAndroidTestCommand)
    }
}
