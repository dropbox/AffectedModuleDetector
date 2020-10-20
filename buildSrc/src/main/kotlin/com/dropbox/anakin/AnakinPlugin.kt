/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */

package com.dropbox.anakin

import org.gradle.api.Plugin
import org.gradle.api.Project

class AnakinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project.isRoot) {
            "Must be applied to root project, but was found on ${project.path} instead."
        }
        project.extensions.add(
            AffectedModuleConfiguration.name,
            AffectedModuleConfiguration()
        )
        AffectedModuleDetector.configure(project.gradle, project)
    }
}
