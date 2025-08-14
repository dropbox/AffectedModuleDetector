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

package com.dropbox.affectedmoduledetector

import com.dropbox.affectedmoduledetector.AffectedModuleDetector.Companion.CHANGED_PROJECTS_ARG
import com.dropbox.affectedmoduledetector.AffectedModuleDetector.Companion.DEPENDENT_PROJECTS_ARG
import com.dropbox.affectedmoduledetector.AffectedModuleDetector.Companion.ENABLE_ARG
import com.dropbox.affectedmoduledetector.AffectedModuleDetector.Companion.MODULES_ARG
import com.dropbox.affectedmoduledetector.commitshaproviders.CommitShaProviderConfiguration
import com.dropbox.affectedmoduledetector.util.toPathSections
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import java.io.File
import java.io.Serializable

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
 * An identifier for a project, ensuring that projects are always identified by their path.
 */
@JvmInline
value class ProjectPath(val path: String) : Serializable

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
abstract class AffectedModuleDetector(protected val logger: Logger?) {
    /**
     * Returns whether this project was affected by current changes.
     *
     * Can only be called during the execution phase
     */
    abstract fun shouldInclude(project: ProjectPath): Boolean

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
    abstract fun isProjectProvided2(project: ProjectPath): Boolean

    /**
     * Returns the set that the project belongs to. The set is one of the ProjectSubset above.
     * This is used by the test config generator.
     *
     * Can be called during the configuration or execution phase
     */
    abstract fun getSubset(project: Project): ProjectSubset

    /**
     * Returns a set of all projects that are affected by the current changes. This includes both
     * projects that have changed files and projects that depend on changed projects.
     */
    abstract fun getAllAffectedProjects(): Set<ProjectPath>

    /**
     * Returns a set of all projects that have changed files
     */
    abstract fun getAllChangedProjects(): Set<ProjectPath>

    companion object {
        private const val ROOT_PROP_NAME = "AffectedModuleDetectorPlugin"
        private const val SERVICE_NAME = ROOT_PROP_NAME + "BuildService"
        private const val MODULES_ARG = "affected_module_detector.modules"
        private const val DEPENDENT_PROJECTS_ARG = "affected_module_detector.dependentProjects"
        private const val CHANGED_PROJECTS_ARG = "affected_module_detector.changedProjects"
        private const val ENABLE_ARG = "affected_module_detector.enable"
        var isConfigured = false

        @JvmStatic
        fun configure(rootProject: Project) {
            require(rootProject == rootProject.rootProject) {
                "Project provided must be root, project was ${rootProject.path}"
            }

            val instance = AffectedModuleDetectorWrapper()
            rootProject.extensions.add(ROOT_PROP_NAME, instance)

            val enabled = isProjectEnabled(rootProject)
            if (!enabled) {
                val provider =
                    setupWithParams(rootProject) { spec ->
                        val params = spec.parameters
                        params.acceptAll = true
                    }
                instance.wrapped = provider
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

            val distDir = if (config.logFolder != null) {
                val distDir = File(config.logFolder!!)
                if (!distDir.exists()) {
                    distDir.mkdirs()
                }
                distDir
            } else {
                rootProject.rootDir
            }

            val outputFile = distDir.resolve(config.logFilename).also {
                it.writeText("")
            }
            val logger = FileLogger(outputFile)

            val modules =
                getModulesProperty(
                    rootProject
                )

            val gitClient = GitClientImpl(
                rootProject.projectDir,
                logger,
                commitShaProviderConfiguration = CommitShaProviderConfiguration(
                    type = config.compareFrom,
                    specifiedBranch = config.specifiedBranch,
                    specifiedSha = config.specifiedRawCommitSha,
                    top = config.top,
                    includeUncommitted = config.includeUncommitted
                ),
                ignoredFiles = config.ignoredFiles
            )

            logger.lifecycle("projects evaluated")
            val projectGraph = ProjectGraph(rootProject)
            val dependencyTracker = DependencyTracker(rootProject, logger.toLogger())
            val provider = setupWithParams(rootProject) { spec ->
                val parameters = spec.parameters
                parameters.acceptAll = false
                parameters.projectGraph = projectGraph
                parameters.dependencyTracker = dependencyTracker
                parameters.log = logger
                parameters.ignoreUnknownProjects = true
                parameters.projectSubset = subset
                parameters.modules = modules
                parameters.config = config
                parameters.gitChangedFilesProvider = gitClient.findChangedFiles(rootProject)
                parameters.gitRoot.set(gitClient.getGitRoot())
            }
            logger.info("Using real detector with $subset")
            instance.wrapped = provider
        }

        private fun setupWithParams(
            rootProject: Project,
            configureAction: Action<BuildServiceSpec<AffectedModuleDetectorLoader.Parameters>>
        ): Provider<AffectedModuleDetectorLoader> {
            if (!rootProject.isRoot) {
                throw IllegalArgumentException("this should've been the root project")
            }
            return rootProject.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                AffectedModuleDetectorLoader::class.java,
                configureAction
            )
        }

        private fun getInstance(project: Project): AffectedModuleDetector? {
            val extensions = project.rootProject.extensions
            val detector = extensions.findByName(ROOT_PROP_NAME) as? AffectedModuleDetector
            return detector!!
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

        internal fun isProjectEnabled(project: Project): Boolean {
            val enabledProvider = project.providers.gradleProperty(ENABLE_ARG)
            return enabledProvider.isPresent && enabledProvider.get() != "false"
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
                ).shouldInclude(task.project.projectPath)
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
            ).shouldInclude(project.projectPath)
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
         * Returns a set of all affected project paths
         *
         * Can only be called during the execution phase
         */
        fun affectedProjects(project: Project): Set<ProjectPath> {
            return getOrThrow(
                project
            ).getAllAffectedProjects()
        }

        /**
         * Returns a set of all changed project paths
         *
         * Can only be called during the execution phase
         */
        fun changedProjects(project: Project): Set<ProjectPath> {
            return getOrThrow(
                project
            ).getAllChangedProjects()
        }

        /**
         * Returns true if the project was provided via [MODULES_ARG] or no [MODULES_ARG] was set
         *
         * Can be called during the configuration or execution phase
         */
        @JvmStatic
        fun isProjectProvided(project: Project): Boolean {
            val rootProject = project.rootProject
            if (!isProjectEnabled(rootProject)) {
                // if we do not want to use affected module detector property then assume every project is provided
                return true
            }
            val modules = getModulesProperty(rootProject)
            return modules?.contains(project.path) ?: true
        }
    }
}

class AffectedModuleDetectorWrapper : AffectedModuleDetector(logger = null) {
    var wrapped: Provider<AffectedModuleDetectorLoader>? = null

    fun getOrThrow(): AffectedModuleDetector {
        return wrapped?.get()?.detector
            ?: throw GradleException(
                """
                        Tried to get the affected module detector implementation too early.
                        You cannot access it until all projects are evaluated.
            """
                    .trimIndent()
            )
    }

    override fun shouldInclude(project: ProjectPath): Boolean {
        return getOrThrow().shouldInclude(project)
    }

    override fun hasAffectedProjects(): Boolean {
        return getOrThrow().hasAffectedProjects()
    }

    override fun isProjectProvided2(project: ProjectPath): Boolean {
        return getOrThrow().isProjectProvided2(project)
    }

    override fun getSubset(project: Project): ProjectSubset {
        return getOrThrow().getSubset(project)
    }

    override fun getAllAffectedProjects(): Set<ProjectPath> {
        return getOrThrow().getAllAffectedProjects()
    }

    override fun getAllChangedProjects(): Set<ProjectPath> {
        return getOrThrow().getAllChangedProjects()
    }
}

abstract class AffectedModuleDetectorLoader :
    BuildService<AffectedModuleDetectorLoader.Parameters> {
    interface Parameters : BuildServiceParameters {
        var acceptAll: Boolean
        var projectGraph: ProjectGraph
        var dependencyTracker: DependencyTracker
        var log: FileLogger
        var ignoreUnknownProjects: Boolean
        var projectSubset: ProjectSubset
        var modules: Set<String>?
        var gitChangedFilesProvider: Provider<List<String>>
        var config: AffectedModuleConfiguration
        val gitRoot: DirectoryProperty
    }

    val detector: AffectedModuleDetector by lazy {
        val logger = parameters.log.toLogger()
        if (parameters.acceptAll) {
            AcceptAll(logger)
        } else {
            AffectedModuleDetectorImpl(
                projectGraph = parameters.projectGraph,
                dependencyTracker = parameters.dependencyTracker,
                logger = logger,
                ignoreUnknownProjects = parameters.ignoreUnknownProjects,
                projectSubset = parameters.projectSubset,
                modules = parameters.modules,
                config = parameters.config,
                changedFilesProvider = parameters.gitChangedFilesProvider,
                gitRoot = parameters.gitRoot.get().asFile
            )
        }
    }
}

/**
 * Implementation that accepts everything without checking.
 */
private class AcceptAll(
    logger: Logger? = null
) : AffectedModuleDetector(logger) {
    override fun shouldInclude(project: ProjectPath): Boolean {
        logger?.info("[AcceptAll] acceptAll.shouldInclude returning true")
        return true
    }

    override fun hasAffectedProjects() = true

    override fun isProjectProvided2(project: ProjectPath) = true

    override fun getSubset(project: Project): ProjectSubset {
        logger?.info("[AcceptAll] AcceptAll.getSubset returning CHANGED_PROJECTS")
        return ProjectSubset.CHANGED_PROJECTS
    }

    override fun getAllAffectedProjects(): Set<ProjectPath> {
        logger?.info("[AcceptAll] AcceptAll.getAllAffectedProjects returning empty set")
        return emptySet()
    }

    override fun getAllChangedProjects(): Set<ProjectPath> {
        logger?.info("[AcceptAll] AcceptAll.getAllChangedProjects returning empty set")
        return emptySet()
    }
}

/**
 * Real implementation that checks git logs to decide what is affected.
 *
 * If any file outside a module is changed, we assume everything has changed.
 *
 * When a file in a module is changed, all modules that depend on it are considered as changed.
 */
class AffectedModuleDetectorImpl(
    private val projectGraph: ProjectGraph,
    private val dependencyTracker: DependencyTracker,
    logger: Logger?,
    // used for debugging purposes when we want to ignore non module files
    private val ignoreUnknownProjects: Boolean = false,
    private val projectSubset: ProjectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
    private val modules: Set<String>? = null,
    private val config: AffectedModuleConfiguration,
    private val changedFilesProvider: Provider<List<String>>,
    private val gitRoot: File,
) : AffectedModuleDetector(logger) {

    init {
        logger?.info("Modules provided: ${modules?.joinToString(separator = ",")}")
    }

    private val allProjects by lazy { projectGraph.allProjects }

    val affectedProjects by lazy {
        findAffectedProjects()
    }

    private val changedProjects by lazy {
        findChangedProjects()
    }

    private val dependentProjects by lazy {
        findDependentProjects()
    }

    private var changedFiles: MutableSet<String> = mutableSetOf()

    private var unknownFiles: MutableSet<String> = mutableSetOf()

    override fun shouldInclude(project: ProjectPath): Boolean {
        val isProjectAffected = affectedProjects.contains(project)
        val isProjectProvided = isProjectProvided2(project)
        val isModuleExcludedByName = config.excludedModules.contains(project.path) || config.excludedModules.contains(project.path.substringAfter(':'))
        val isModuleExcludedByRegex = config.excludedModules.any { project.path.matches(it.toRegex()) }
        val isNotModuleExcluded = !(isModuleExcludedByName || isModuleExcludedByRegex)

        val shouldInclude = isProjectAffected && isProjectProvided && isNotModuleExcluded
        logger?.info("checking whether I should include ${project.path} and my answer is $shouldInclude")

        return shouldInclude
    }

    override fun hasAffectedProjects() = affectedProjects.isNotEmpty()

    override fun isProjectProvided2(project: ProjectPath): Boolean {
        if (modules == null) return true
        return modules.contains(project.path)
    }

    override fun getSubset(project: Project): ProjectSubset {
        return when {
            changedProjects.contains(project.projectPath) -> {
                ProjectSubset.CHANGED_PROJECTS
            }
            dependentProjects.contains(project.projectPath) -> {
                ProjectSubset.DEPENDENT_PROJECTS
            }
            else -> {
                ProjectSubset.NONE
            }
        }
    }

    override fun getAllAffectedProjects(): Set<ProjectPath> {
        return affectedProjects
    }

    override fun getAllChangedProjects(): Set<ProjectPath> {
        return changedProjects
    }

    /**
     * Finds only the set of projects that were directly changed in the commit.
     *
     * Also populates the unknownFiles var which is used in findAffectedProjects
     */
    private fun findChangedProjects(): Set<ProjectPath> {
        (changedFilesProvider.getOrNull() ?: return allProjects).forEach { fileName ->
            if (affectsAllModules(fileName)) {
                logger?.info("File $fileName affects all modules")
                return allProjects
            }
            changedFiles.add(fileName)
        }

        val changedProjects = mutableSetOf<ProjectPath>()

        for (filePath in changedFiles) {
            val containingProject = findContainingProject(filePath)
            if (containingProject == null) {
                unknownFiles.add(filePath)
                logger?.info(
                    "Couldn't find containing project for file $filePath. " +
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
    private fun findDependentProjects(): Set<ProjectPath> {
        return changedProjects.flatMap { path ->
            dependencyTracker.findAllDependents(path)
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
    private fun findAffectedProjects(): Set<ProjectPath> {
        // In this case we don't care about any of the logic below, we're only concerned with
        // running the changed projects in this test runner
        if (projectSubset == ProjectSubset.CHANGED_PROJECTS) {
            return changedProjects
        }

        var buildAll = false

        // Should only trigger if there are no changedFiles
        if (config.buildAllWhenNoProjectsChanged && changedProjects.isEmpty() && unknownFiles.isEmpty()) {
            buildAll = true
        }
        logger?.info(
            "unknownFiles: $unknownFiles, changedProjects: $changedProjects, buildAll: " +
                "$buildAll"
        )

        // If we're in a buildAll state, we return allProjects unless it's the changed target,
        // Since the changed target runs all tests and we don't want 3+ hour presubmit runs
        if (buildAll) {
            logger?.info("Building all projects because no changed files were detected")
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

    private fun affectsAllModules(relativeFilePath: String): Boolean {
        val rootProjectDir = if (config.baseDir != null) {
            File(config.baseDir!!)
        } else {
            File(projectGraph.getRootProjectPath()!!.path)
        }
        val pathSections = relativeFilePath.toPathSections(rootProjectDir, gitRoot)
        val projectRelativePath = pathSections.joinToString(File.separatorChar.toString())

        return config.pathsAffectingAllModules.any { projectRelativePath.startsWith(it) }
    }

    private fun findContainingProject(filePath: String): ProjectPath? {
        return projectGraph.findContainingProject(filePath, logger).also {
            logger?.info("search result for $filePath resulted in ${it?.path}")
        }
    }
}

val Project.isRoot get() = this == rootProject

val Project.projectPath: ProjectPath get() = ProjectPath(path)
