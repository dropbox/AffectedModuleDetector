package com.dropbox.test

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/**
 * Example plugin which uses the [AffectedModuleDetector] to filter out tests which don't need to run
 *
 * This is something a developer would need to build to use the [AffectedModuleDetector].
 */
class AffectedTestsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Only allow unit tests to run if the AffectedModuleDetector says to include them
        target.tasks.withType(Test::class.java) { task ->
            // AffectedModuleDetector.configureTaskGuard(task)
        }
    }
}
