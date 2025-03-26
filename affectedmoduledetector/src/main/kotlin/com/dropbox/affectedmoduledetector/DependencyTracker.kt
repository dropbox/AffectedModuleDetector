/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
package com.dropbox.affectedmoduledetector

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import java.io.Serializable

/**
 * Utility class that traverses all project dependencies and discover which modules depend on each
 * other. This is mainly used by [AffectedModuleDetector] to find out which projects should be run.
 */
class DependencyTracker(rootProject: Project, logger: Logger?) : Serializable {
    val dependentList: Map<ProjectPath, Set<ProjectPath>>

    init {
        val result = mutableMapOf<ProjectPath, MutableSet<ProjectPath>>()
        val stringBuilder = StringBuilder()
        rootProject.subprojects.forEach { project ->
            project.configurations.forEach { config ->
                config.dependencies.filterIsInstance<ProjectDependency>().forEach {
                    stringBuilder.append(
                        "there is a dependency from ${project.path} (${config.name}) to " +
                            it.dependencyProject.path +
                            "\n"
                    )
                    result.getOrPut(it.dependencyProject.projectPath) { mutableSetOf() }.add(project.projectPath)
                }
            }
        }
        logger?.info(stringBuilder.toString())
        dependentList = result
    }

    fun findAllDependents(projectPath: ProjectPath): Set<ProjectPath> {
        val result = mutableSetOf<ProjectPath>()
        fun addAllDependents(projectPath: ProjectPath) {
            if (result.add(projectPath)) {
                dependentList[projectPath]?.forEach(::addAllDependents)
            }
        }
        addAllDependents(projectPath)
        // the projectPath isn't a dependent of itself
        return result.minus(projectPath)
    }
}
