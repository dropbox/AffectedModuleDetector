package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class AffectedTestConfigurationTest {

    private lateinit var config: AffectedTestConfiguration

    @Before
    fun setup() {
        config = AffectedTestConfiguration()
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN default values THEN default values returned`() {
        val taskNames = config.getTestTaskNames()
        assertThat(taskNames.assembleAndroidTestTasks.size).isEqualTo(1)
        assertThat(taskNames.assembleAndroidTestTasks.first()).isEqualTo("assembleDebugAndroidTest")
        assertThat(taskNames.androidTestTasks.size).isEqualTo(1)
        assertThat(taskNames.androidTestTasks.first()).isEqualTo("connectedDebugAndroidTest")
        assertThat(taskNames.unitTestTasks.size).isEqualTo(1)
        assertThat(taskNames.unitTestTasks.first()).isEqualTo("testDebugUnitTest")
    }

    @Test
    fun `GIVEN AffectedTestConfiguration WHEN companion object name is called THEN affectedTestConfiguration is returned`() {
        assertThat(AffectedTestConfiguration.name).isEqualTo("affectedTestConfiguration")
    }
}
