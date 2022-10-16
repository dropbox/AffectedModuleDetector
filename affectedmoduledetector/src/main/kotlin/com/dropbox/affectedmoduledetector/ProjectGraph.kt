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

import com.dropbox.affectedmoduledetector.util.toPathSections
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Creates a project graph for fast lookup by file path
 */
internal class ProjectGraph(project: Project, val gitRoot: File, val logger: Logger? = null) {
    private val rootNode: Node
    private val rootProjectDir: File

    init {
        logger?.info("initializing ProjectGraph")
        rootNode = Node(logger)
        rootProjectDir = project.getSupportRootFolder().canonicalFile
        project.subprojects.forEach {
            logger?.info("creating node for ${it.path}")
            val relativePath = it.projectDir.canonicalFile.toRelativeString(rootProjectDir)
            val sections = relativePath.split(File.separatorChar)
            logger?.info("relative path: $relativePath , sections: $sections")
            val leaf = sections.fold(rootNode) { left, right ->
                left.getOrCreateNode(right)
            }
            leaf.project = it
        }
        logger?.info("finished creating ProjectGraph $rootNode")
    }

    /**
     * Finds the project that contains the given file.
     * The file's path prefix should match the project's path.
     */
    fun findContainingProject(relativeFilePath: String): Project? {
        val pathSections = relativeFilePath.toPathSections(rootProjectDir, gitRoot)

        logger?.info("finding containing project for $relativeFilePath , sections: $pathSections")
        return rootNode.find(pathSections, 0)
    }

    private class Node(val logger: Logger? = null) {
        var project: Project? = null
        private val children = mutableMapOf<String, Node>()

        fun getOrCreateNode(key: String): Node {
            return children.getOrPut(key) {
                Node(
                    logger
                )
            }
        }

        fun find(sections: List<String>, index: Int): Project? {
            logger?.info("finding $sections with index $index in ${project?.path ?: "root"}")
            if (sections.size <= index) {
                logger?.info("nothing")
                return project
            }
            val child = children[sections[index]]
            return if (child == null) {
                logger?.info("no child found, returning ${project?.path ?: "root"}")
                project
            } else {
                child.find(sections, index + 1)
            }
        }
    }
}

/**
 * Returns the path to the canonical root project directory, e.g. {@code frameworks/support}.
 */
fun Project.getSupportRootFolder(): File = project.rootDir
