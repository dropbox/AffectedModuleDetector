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

    private val configuration: AffectedModuleConfiguration by lazy {
        rootProject.extensions.getByType(AffectedModuleConfiguration::class.java)
    }

    private val dependentList: Map<ProjectPath, Set<ProjectPath>> by lazy {
        val result = mutableMapOf<ProjectPath, MutableSet<ProjectPath>>()
        rootProject.subprojects.forEach { project ->
            logger?.info("checking ${project.path} for dependencies")
            project.configurations
                .filter(configuration.configurationPredicate::test)
                .forEach { config ->
                    logger?.info("checking config ${project.path}/$config for dependencies")
                    config
                        .dependencies
                        .filterIsInstance<ProjectDependency>()
                        .forEach {
                            logger?.info(
                                "there is a dependency from ${project.projectPath} to " +
                                        it.path,
                            )
                            result.getOrPut(ProjectPath(it.path)) { mutableSetOf() }
                                .add(project.projectPath)
                        }
                }
        }
        result
    }

    fun findAllDependents(projectPath: ProjectPath, logger: Logger? = null): Set<ProjectPath> {
        logger?.info("finding dependents of $projectPath")
        val result = mutableSetOf<ProjectPath>()
        fun addAllDependents(projectPath: ProjectPath) {
            if (result.add(projectPath)) {
                dependentList[projectPath]?.forEach(::addAllDependents)
            }
        }
        addAllDependents(projectPath)
        logger?.info("dependents of $projectPath is $result")
        // the project isn't a dependent of itself
        return result.minus(projectPath)
    }
}
