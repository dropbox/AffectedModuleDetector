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
import org.gradle.api.logging.Logger
import java.io.File
import java.io.Serializable

/** Creates a project graph for fast lookup by file path */
class ProjectGraph(project: Project, logger: Logger? = null) : Serializable {
    private val rootNode: Node

    init {
        // always use cannonical file: b/112205561
        logger?.info("initializing ProjectGraph")
        rootNode = Node()
        val rootProjectDir = project.getSupportRootFolder().canonicalFile
        val projects =
            if (rootProjectDir == project.rootDir.canonicalFile) {
                project.subprojects
            } else {
                // include root project if it is not the main AndroidX project.
                project.subprojects + project
            }
        projects.forEach {
            logger?.info("creating node for ${it.path}")
            val relativePath = it.projectDir.canonicalFile.toRelativeString(rootProjectDir)
            val sections = relativePath.split(File.separatorChar)

            // If the subproject is not a child of the root project (in the File directory sense)
            // then we are in some weird non standard quixotic project and our sections are going
            // to have ".." characters indicating that we need to traverse up one level. However
            // we need to filter these out because this will not match the parent child
            // dependency relationship that Gradle will produce.
            val realSections = sections.filter { section -> section != ".." }

            logger?.info("relative path: $relativePath , sections: $realSections")
            val leaf = realSections.fold(rootNode) { left, right ->
                left.getOrCreateNode(right)
            }
            leaf.projectPath = it.path
        }
        logger?.info("finished creating ProjectGraph")
    }

    /**
     * Finds the project that contains the given file. The file's path prefix should match the
     * project's path.
     */
    fun findContainingProject(filePath: String, logger: Logger? = null): ProjectPath? {
        val sections = filePath.split(File.separatorChar)
        logger?.info("finding containing project for $filePath , sections: $sections")
        return rootNode.find(sections, 0, logger)?.let { ProjectPath(it) }
    }

    val allProjects by lazy {
        val result = mutableSetOf<String>()
        rootNode.addAllProjectPaths(result)
        result.map { ProjectPath(it) }.toSet()
    }

    private class Node() : Serializable {
        var projectPath: String? = null
        private val children = mutableMapOf<String, Node>()

        fun getOrCreateNode(key: String): Node {
            return children.getOrPut(key) { Node() }
        }

        fun find(sections: List<String>, index: Int, logger: Logger?): String? {
            if (sections.size <= index) {
                logger?.info("nothing")
                return projectPath
            }
            val child = children[sections[index]]
            return if (child == null) {
                logger?.info("no child found, returning ${projectPath ?: "root"}")
                projectPath
            } else {
                child.find(sections, index + 1, logger)
            }
        }

        fun addAllProjectPaths(collection: MutableSet<String>) {
            projectPath?.let { path -> collection.add(path) }
            for (child in children.values) {
                child.addAllProjectPaths(collection)
            }
        }
    }

    fun getRootProjectPath(): ProjectPath? {
        return rootNode.projectPath?.let { ProjectPath(it) }
    }
}

fun Project.getSupportRootFolder(): File = project.rootDir
