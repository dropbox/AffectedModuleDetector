package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class AffectedTestConfigurationTest {

    private lateinit var config : AffectedTestConfiguration

    @Before
    fun setup() {
        config = AffectedTestConfiguration()
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN default values THEN default values returned`() {
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
        config.assembleAndroidTestTask = assembleAndroidTestTask
        config.runAndroidTestTask = runAndroidTestTask
        config.jvmTestTask = jvmTest

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
