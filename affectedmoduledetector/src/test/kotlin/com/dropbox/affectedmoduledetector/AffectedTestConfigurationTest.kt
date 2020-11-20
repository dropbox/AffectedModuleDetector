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
    fun `GIVEN AffectedTestConfiguration WHEN default value of variant to test THEN debug is returned`() {
        assertThat(config.variantToTest).isEqualTo("debug")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN variant to test is set THEN value is returned`() {
        // GIVEN
        val sample = "sample"

        // WHEN
        config.variantToTest = sample

        // THEN
        assertThat(config.variantToTest).isEqualTo(sample)
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN assemble android test task is called THEN default is returned`() {
        assertThat(config.assembleAndroidTestTask).isEqualTo("assembleDebugAndroidTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN variant is setup and assemble android test task is called THEN variant task is returned`() {
        // GIVEN
        config.variantToTest = "sample"

        // WHEN
        val task = config.assembleAndroidTestTask

        // THEN
        assertThat(task).isEqualTo("assembleSampleAndroidTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN run android test task is called THEN default is returned`() {
        assertThat(config.runAndroidTestTask).isEqualTo("connectedDebugAndroidTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN variant is setup and run android test task is called THEN variant task is returned`() {
        // GIVEN
        config.variantToTest = "sample"

        // WHEN
        val task = config.runAndroidTestTask

        // THEN
        assertThat(task).isEqualTo("connectedSampleAndroidTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN jvm test is called THEN default is returned`() {
        assertThat(config.jvmTest).isEqualTo("testDebugUnitTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN variant is setup and jvm test is called THEN variant task is returned`() {
        // GIVEN
        config.variantToTest = "sample"

        // WHEN
        val task = config.jvmTest

        // THEN
        assertThat(task).isEqualTo("testSampleUnitTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN companion object name is called THEN affectedTestConfiguration is returned`() {
        assertThat(AffectedTestConfiguration.name).isEqualTo("affectedTestConfiguration")
    }
}
