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
@file:JvmName("AffectedModuleDetectorUtil")

package com.dropbox.detector

import com.dropbox.detector.AffectedModuleDetector.Companion.CHANGED_PROJECTS_ARG
import com.dropbox.detector.AffectedModuleDetector.Companion.DEPENDENT_PROJECTS_ARG
import com.dropbox.detector.AffectedModuleDetector.Companion.ENABLE_ARG
import com.dropbox.detector.AffectedModuleDetector.Companion.MODULES_ARG
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import java.io.File
import java.util.UUID

/**
 * The subsets we allow the projects to be partitioned into.
 * This is to allow more granular testing. Specifically, to enable running large tests on
 * CHANGED_PROJECTS, while still only running small and medium tests on DEPENDENT_PROJECTS.
 *
 * The ProjectSubset specifies which projects we are interested in testing.
 * The AffectedModuleDetector determines the minimum set of projects that must be built in
 * order to run all the tests along with their runtime dependencies.
 *
 * The subsets are:
 *  CHANGED_PROJECTS -- The containing projects for any files that were changed.
 *
 *  DEPENDENT_PROJECTS -- Any projects that have a dependency on any of the projects
 *      in the CHANGED_PROJECTS set.
 *
 *  ALL_AFFECTED_PROJECTS -- The union of CHANGED_PROJECTS and DEPENDENT_PROJECTS,
 *      which encompasses all projects that could possibly break due to the changes.
 *
 *  NONE -- A status to return for a project when it is not supposed to be built.
 */
enum class ProjectSubset { DEPENDENT_PROJECTS, CHANGED_PROJECTS, ALL_AFFECTED_PROJECTS, NONE }

/**
 * A utility class that can discover which files are changed based on git history.
 *
 * To enable this, you need to pass [ENABLE_ARG] into the build as a command line parameter
 * (-P<name>)
 *
 * Passing [DEPENDENT_PROJECTS_ARG] will result in only DEPENDENT_PROJECTS being returned (see enum)
 * Passing [CHANGED_PROJECTS_ARG] will behave likewise.
 *
 * If neither of those are passed, ALL_AFFECTED_PROJECTS is returned.
 *
 * [MODULES_ARG] takes a comma delimited list of paths.  If this is provided, it will ensure both a
 * module is affected per the [ProjectSubset] rules, and included in the list.  If it is not
 * provided, it will follow [ProjectSubset]
 *
 * Currently, it checks git logs to find the files changed in the last commit
 *
 * Since this needs to check project dependency graph to work, it cannot be accessed before
 * all projects are loaded. Doing so will throw an exception.
 */
abstract class AffectedModuleDetector {
    /**
     * Returns whether this project was affected by current changes.
     *
     * Can only be called during the execution phase
     */
    abstract fun shouldInclude(project: Project): Boolean

    /**
     * Returns true if at least one project has been affected
     *
     * Can only be called during the execution phase
     */
    abstract fun hasAffectedProjects(): Boolean

    /**
     * Returns true if the project was provided via [MODULES_ARG] or no [MODULES_ARG] was set
     *
     * Can be called during the configuration or execution phase
     */
    abstract fun isProjectProvided(path: String): Boolean

    /**
     * Returns the set that the project belongs to. The set is one of the ProjectSubset above.
     * This is used by the test config generator.
     *
     * Can be called during the configuration or execution phase
     */
    abstract fun getSubset(project: Project): ProjectSubset

    companion object {
        private const val ROOT_PROP_NAME = "AffectedModuleDetectorPlugin"
        internal const val MODULES_ARG = "affected_module_detector.modules"
        private const val DEPENDENT_PROJECTS_ARG = "affected_module_detector.dependentProjects"
        private const val CHANGED_PROJECTS_ARG = "affected_module_detector.changedProjects"
        private const val ENABLE_ARG = "affected_module_detector.enable"
        var isConfigured = false

        @JvmStatic
        fun configure(gradle: Gradle, rootProject: Project) {
            val enabled = rootProject.hasProperty(ENABLE_ARG)
            if (!enabled) {
                setInstance(
                    rootProject,
                    AcceptAll()
                )
                return
            }
            isConfigured = true

            val subset = when {
                rootProject.hasProperty(DEPENDENT_PROJECTS_ARG) -> {
                    ProjectSubset.DEPENDENT_PROJECTS
                }
                rootProject.hasProperty(CHANGED_PROJECTS_ARG) -> {
                    ProjectSubset.CHANGED_PROJECTS
                }
                else -> {
                    ProjectSubset.ALL_AFFECTED_PROJECTS
                }
            }

            val config =
                requireNotNull(
                    rootProject.extensions.findByType(AffectedModuleConfiguration::class.java)
                ) {
                    "Root project ${rootProject.path} must have the AffectedModuleConfiguration " +
                        "extension added."
                }

            val logger =
                ToStringLogger.createWithLifecycle(
                    gradle
                ) { log ->
                    config.logFolder?.let {
                        val distDir = File(it)
                        if (!distDir.exists()) {
                            distDir.mkdirs()
                        }
                        val outputFile =
                            distDir.resolve(config.logFilename)
                        outputFile.appendText(log)
                        println("Wrote dependency log to ${outputFile.absolutePath}")
                    }
                }

            val modules =
                getModulesProperty(
                    rootProject
                )

            AffectedModuleDetectorImpl(
                rootProject = rootProject,
                logger = logger,
                ignoreUnknownProjects = true,
                projectSubset = subset,
                modules = modules,
                config = config
            ).also {
                logger.info("Using real detector")
                setInstance(
                    rootProject,
                    it
                )
            }
        }

        private fun setInstance(
            rootProject: Project,
            detector: AffectedModuleDetector
        ) {
            if (!rootProject.isRoot) {
                throw IllegalArgumentException(
                    "This should've been the root project, instead found ${rootProject.path}"
                )
            }
            rootProject.extensions.add(ROOT_PROP_NAME, detector)
        }

        private fun getInstance(project: Project): AffectedModuleDetector? {
            val extensions = project.rootProject.extensions

            return extensions.getByName(ROOT_PROP_NAME) as? AffectedModuleDetector
        }

        private fun getOrThrow(project: Project): AffectedModuleDetector {

            return getInstance(
                project
            ) ?: throw GradleException(
                """
                        Tried to get affected module detector too early.
                        You cannot access it until all projects are evaluated.
                """.trimIndent()
            )
        }

        private fun getModulesProperty(project: Project): Set<String>? {
            return if (project.hasProperty(MODULES_ARG)) {
                val commaDelimited = project.properties[MODULES_ARG] as String
                commaDelimited.split(",").toSet()
            } else {
                null
            }
        }

        /**
         * Call this method to configure the given task to execute only if the owner project
         * is affected by current changes
         *
         * Can be called during the configuration or execution phase
         */
        @Throws(GradleException::class)
        @JvmStatic
        fun configureTaskGuard(task: Task) {
            task.onlyIf {
                getOrThrow(
                    task.project
                ).shouldInclude(task.project)
            }
        }

        /**
         * Call this method to determine if the project was affected in this change
         *
         * Can only be called during the execution phase
         */
        @JvmStatic
        @Throws(GradleException::class)
        fun isProjectAffected(project: Project): Boolean {
            return getOrThrow(
                project
            ).shouldInclude(project)
        }

        /**
         * Call this method to determine if root project has at least one affected project
         *
         * Can only be called during the execution phase
         */
        @Throws(GradleException::class)
        fun hasAffectedProjects(project: Project): Boolean {
            return getOrThrow(
                project
            ).hasAffectedProjects()
        }

        /**
         * Returns true if the project was provided via [MODULES_ARG] or no [MODULES_ARG] was set
         *
         * Can be called during the configuration or execution phase
         */
        @JvmStatic
        fun isProjectProvided(project: Project): Boolean {
            return getOrThrow(
                project
            ).isProjectProvided(project.path)
        }
    }
}

/**
 * Implementation that accepts everything without checking.
 */
private class AcceptAll(
    private val wrapped: AffectedModuleDetector? = null,
    private val logger: Logger? = null
) : AffectedModuleDetector() {
    override fun shouldInclude(project: Project): Boolean {
        val wrappedResult = wrapped?.shouldInclude(project)
        logger?.info("[AcceptAll] wrapper returned $wrappedResult but I'll return true")
        return true
    }

    override fun hasAffectedProjects() = true

    override fun isProjectProvided(path: String) = true

    override fun getSubset(project: Project): ProjectSubset {
        val wrappedResult = wrapped?.getSubset(project)
        logger?.info("[AcceptAll] wrapper returned $wrappedResult but I'll return CHANGED_PROJECTS")
        return ProjectSubset.CHANGED_PROJECTS
    }
}

/**
 * Real implementation that checks git logs to decide what is affected.
 *
 * If any file outside a module is changed, we assume everything has changed.
 *
 * When a file in a module is changed, all modules that depend on it are considered as changed.
 */
class AffectedModuleDetectorImpl constructor(
    private val rootProject: Project,
    private val logger: Logger?,
    // used for debugging purposes when we want to ignore non module files
    private val ignoreUnknownProjects: Boolean = false,
    private val projectSubset: ProjectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
    private val injectedGitClient: GitClient? = null,
    private val modules: Set<String>? = null,
    private val config: AffectedModuleConfiguration
) : AffectedModuleDetector() {

    init {
        logger?.info("Modules provided: ${modules?.joinToString(separator = ",")}")
    }

    private val git by lazy {
        injectedGitClient ?: GitClientImpl(
            rootProject.projectDir,
            logger
        )
    }

    private val dependencyTracker by lazy {
        DependencyTracker(rootProject, logger)
    }

    private val allProjects by lazy {
        rootProject.subprojects.toSet()
    }

    private val projectGraph by lazy {
        ProjectGraph(rootProject, git.getGitRoot(), logger)
    }

    val affectedProjects by lazy {
        findAffectedProjects()
    }

    private val changedProjects by lazy {
        findChangedProjects()
    }

    private val dependentProjects by lazy {
        findDependentProjects()
    }

    private var unknownFiles: MutableSet<String> = mutableSetOf()

    override fun shouldInclude(project: Project): Boolean {
        return (
            project.isRoot || (
                affectedProjects.contains(project) &&
                    isProjectProvided(project.path)
                )
            ).also {
            logger?.info(
                "checking whether I should include ${project.path} and my answer is $it"
            )
        }
    }

    override fun hasAffectedProjects() = affectedProjects.isNotEmpty()

    override fun isProjectProvided(path: String): Boolean {
        return (modules == null || modules.contains(path))
    }

    override fun getSubset(project: Project): ProjectSubset {
        return when {
            changedProjects.contains(project) -> {
                ProjectSubset.CHANGED_PROJECTS
            }
            dependentProjects.contains(project) -> {
                ProjectSubset.DEPENDENT_PROJECTS
            }
            else -> {
                ProjectSubset.NONE
            }
        }
    }

    /**
     * Finds only the set of projects that were directly changed in the commit.
     *
     * Also populates the unknownFiles var which is used in findAffectedProjects
     */
    private fun findChangedProjects(): Set<Project> {
        val lastMergeSha = git.findPreviousCommitSha() ?: return allProjects
        val changedFiles = git.findChangedFilesSince(
            sha = lastMergeSha,
            includeUncommitted = true
        )

        val changedProjects = mutableSetOf<Project>()

        for (filePath in changedFiles) {
            val containingProject = findContainingProject(filePath)
            if (containingProject == null) {
                unknownFiles.add(filePath)
                logger?.info(
                    "Couldn't find containing project for file$filePath. " +
                        "Adding to unknownFiles."
                )
            } else {
                changedProjects.add(containingProject)
                logger?.info(
                    "For file $filePath containing project is $containingProject. " +
                        "Adding to changedProjects."
                )
            }
        }

        return changedProjects
    }

    /**
     * Gets all dependent projects from the set of changedProjects. This doesn't include the
     * original changedProjects. Always build is still here to ensure at least 1 thing is built
     */
    private fun findDependentProjects(): Set<Project> {
        return changedProjects.flatMap {
            dependencyTracker.findAllDependents(it)
        }.toSet()
    }

    /**
     * By default, finds all modules that are affected by current changes
     *
     * With param dependentProjects, finds only modules dependent on directly changed modules
     *
     * With param changedProjects, finds only directly changed modules
     *
     * If it cannot determine the containing module for a file (e.g. buildSrc or root), it
     * defaults to all projects unless [ignoreUnknownProjects] is set to true.
     *
     * Also detects modules whose tests are codependent at runtime.
     */
    @Suppress("ComplexMethod")
    private fun findAffectedProjects(): Set<Project> {
        // In this case we don't care about any of the logic below, we're only concerned with
        // running the changed projects in this test runner
        if (projectSubset == ProjectSubset.CHANGED_PROJECTS) {
            return changedProjects
        }

        var buildAll = false

        // Should only trigger if there are no changedFiles
        if (changedProjects.isEmpty() && unknownFiles.isEmpty()) {
            buildAll = true
        }
        unknownFiles.forEach {
            if (affectsAllModules(it)) {
                buildAll = true
            }
        }
        logger?.info(
            "unknownFiles: $unknownFiles, changedProjects: $changedProjects, buildAll: " +
                "$buildAll"
        )

        // If we're in a buildAll state, we return allProjects unless it's the changed target,
        // Since the changed target runs all tests and we don't want 3+ hour presubmit runs
        if (buildAll) {
            logger?.info("Building all projects")
            if (unknownFiles.isEmpty()) {
                logger?.info("because no changed files were detected")
            } else {
                logger?.info("because one of the unknown files affects everything in the build")
                logger?.info(
                    """
                    The modules detected as affected by changed files are
                    ${changedProjects + dependentProjects}
                    """.trimIndent()
                )
            }
            when (projectSubset) {
                ProjectSubset.DEPENDENT_PROJECTS -> return allProjects
                ProjectSubset.ALL_AFFECTED_PROJECTS -> return allProjects
                else -> {
                }
            }
        }

        return when (projectSubset) {
            ProjectSubset.ALL_AFFECTED_PROJECTS -> changedProjects + dependentProjects
            ProjectSubset.CHANGED_PROJECTS -> changedProjects
            else -> dependentProjects
        }
    }

    private fun affectsAllModules(file: String): Boolean {
        logger?.info("Paths affecting all modules: ${config.pathsAffectingAllModules}")
        return config.pathsAffectingAllModules.any { file.startsWith(it) }
    }

    private fun findContainingProject(filePath: String): Project? {
        return projectGraph.findContainingProject(filePath).also {
            logger?.info("search result for $filePath resulted in ${it?.path}")
        }
    }
}

class AffectedModuleConfiguration {
    /**
     * Folder to place the log in
     */
    var logFolder: String? = null

    /**
     * Name for the log file
     */
    var logFilename: String = "affected_module_detector.log"

    /**
     * Base directory to use for [pathsAffectingAllModules]
     */
    var baseDir: String? = null
    /**
     * Files or folders which if changed will trigger all projects to be considered affected
     */
    var pathsAffectingAllModules = setOf<String>()
        set(value) {
            requireNotNull(baseDir) {
                "baseDir must be set to use pathsAffectingAllModules"
            }
            field = value
        }
        get() {
            field.forEach { path ->
                require(File(baseDir, path).exists()) {
                    "Could not find expected path in pathsAffectingAllModules: $path"
                }
            }
            return field
        }

    companion object {
        const val name = "affectedModuleDetector"
    }
}

val Project.isRoot get() = this == rootProject
