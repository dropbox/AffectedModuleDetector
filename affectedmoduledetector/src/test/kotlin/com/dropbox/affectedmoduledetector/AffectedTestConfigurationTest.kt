package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AffectedTestConfigurationTest {

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN default values THEN default values returned`() {
        val config = AffectedTestConfiguration()
        assertThat(config.assembleAndroidTestTask).isEqualTo("assembleDebugAndroidTest")
        assertThat(config.runAndroidTestTask).isEqualTo("connectedDebugAndroidTest")
        assertThat(config.jvmTestTask).isEqualTo("testDebugUnitTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN values are updated THEN new values are returned`() {
        // GIVEN
        val assembleAndroidTestTask = "assembleAndroidTestTask"
        val runAndroidTestTask = "runAndroidTestTask"
        val jvmTest = "jvmTest"

        // WHEN
        val config = AffectedTestConfiguration(
            assembleAndroidTestTask,
            runAndroidTestTask,
            jvmTest,
        )

        // THEN
        assertThat(config.assembleAndroidTestTask).isEqualTo(assembleAndroidTestTask)
        assertThat(config.runAndroidTestTask).isEqualTo(runAndroidTestTask)
        assertThat(config.jvmTestTask).isEqualTo(jvmTest)
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN companion object name is called THEN affectedTestConfiguration is returned`() {
        assertThat(AffectedTestConfiguration.name).isEqualTo("affectedTestConfiguration")
    }
}
