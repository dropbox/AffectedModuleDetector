package com.dropbox.affectedmoduledetector.rules

import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

/**
 * TestRule that allows setup of the Android SDK within the context of a GradleRunner test.
 */
class SetupAndroidProject : TemporaryFolder() {

    /**
     * Setup the Android SDK location for test involving Gradle and the Android Gradle DSL.
     *
     * Users may have configured their machine with an environment variable ANDROID_SDK_ROOT in which case the
     * Android SDK will automatically be found using this. Otherwise we attempt to local the machine specific
     * local.properties file and copy the contents.
     */
    fun setupAndroidSdkLocation() {
        // If we happen to have already set this environment variable then we are all good to go.
        // Nothing to see here. Move along.
        if (!System.getenv("ANDROID_SDK_ROOT").isNullOrBlank()) return

        // Find the local.properties file. File works relative to the nearest build.gradle which is the one for the
        // Gradle module that we are currently running tests in - not the root project. But assuming that we will only
        // ever be one level deep then we can simply use ".." to go up one level where we _should_ find our file.
        val localDotProperties = File("../local.properties")

        if (!localDotProperties.exists()) throw FileNotFoundException(
            """Unable to locate Android SDK. Ensure that you have either the ANDROID_SDK_ROOT environment variable 
            |set or the sdk.dir location correctly set in local.properties within the Gradle root project directory.
            |""".trimMargin()
        )

        // Read our machine specific local.properties file and copy it to our temp Gradle test directory for the test.
        localDotProperties.bufferedReader().use {
            newFile("local.properties").writeText(it.readText())
        }
    }
}
