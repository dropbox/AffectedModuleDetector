package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class AffectedModuleDetectorImplTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger by lazy { attachLogsRule.logger }

    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Rule
    @JvmField
    val tmpFolder2 = TemporaryFolder()

    @Rule
    @JvmField
    val tmpFolder3 = TemporaryFolder()

    private lateinit var root: Project
    private lateinit var rootProjectGraph: ProjectGraph
    private lateinit var rootDependencyTracker: DependencyTracker
    private lateinit var root2: Project
    private lateinit var root2ProjectGraph: ProjectGraph
    private lateinit var root2DependencyTracker: DependencyTracker
    private lateinit var root3: Project
    private lateinit var root3ProjectGraph: ProjectGraph
    private lateinit var root3DependencyTracker: DependencyTracker
    private lateinit var p1: Project
    private lateinit var p2: Project
    private lateinit var p3: Project
    private lateinit var p4: Project
    private lateinit var p5: Project
    private lateinit var p6: Project
    private lateinit var p7: Project
    private lateinit var p8: Project
    private lateinit var p9: Project
    private lateinit var p10: Project
    private lateinit var p12: Project
    private lateinit var p13: Project
    private lateinit var p16: Project
    private lateinit var p17: Project
    private lateinit var p18: Project
    private lateinit var p19: Project
    private val pathsAffectingAllModules = setOf(
        convertToFilePath("tools", "android", "buildSrc"),
        convertToFilePath("android", "gradlew"),
        convertToFilePath("android", "gradle"),
        convertToFilePath("dbx", "core", "api")
    )
    private lateinit var affectedModuleConfiguration: AffectedModuleConfiguration

    @Before
    fun init() {
        val tmpDir = tmpFolder.root
        val tmpDir2 = tmpFolder2.root
        val tmpDir3 = tmpFolder3.root

        pathsAffectingAllModules.forEach {
            File(tmpDir, it).mkdirs()
        }

        /*
        d: File directories
        p: Gradle projects

        Dummy project file tree:
           "library modules"                  "UI modules"           "quixotic project"
              tmpDir --------------             tmpDir2                    tmpDir3
              / |  \     |   |    |             /    \                    /   |    \
            d1  d7  d2  d8   d9  d10           d12   d13 (d10)         d14  root3   d15
           /         \                                                 /          /  |  \
          d3          d5                                              d16       d17  d18  d19
         /  \
       d4   d6

        Dependency forest:
               root -------------------           root2                 root3 --------
              /    \     |    |   |   |           /   \                /  |  \       |
            p1     p2    p7  p8  p9  p10         p12  p13           p16 - | - p18 - p19
           /      /  \                                                 \  |
          p3 --- p5   p6                                                 p17
         /
        p4

         */

        // Root projects
        root = ProjectBuilder.builder()
            .withProjectDir(tmpDir)
            .withName("root")
            .build()
        // Project Graph expects supportRootFolder.
        (root.properties["ext"] as ExtraPropertiesExtension).set("supportRootFolder", tmpDir)
        root2 = ProjectBuilder.builder()
            .withProjectDir(tmpDir2)
            .withName("root2/ui")
            .build()
        // Project Graph expects supportRootFolder.
        (root2.properties["ext"] as ExtraPropertiesExtension).set("supportRootFolder", tmpDir2)
        root3 = ProjectBuilder.builder()
            .withProjectDir(tmpDir3.resolve("root3"))
            .withName("root3")
            .build()
        // Project Graph expects supportRootFolder.
        (root3.properties["ext"] as ExtraPropertiesExtension).set("supportRootFolder", tmpDir3)

        // Library modules
        p1 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d1"))
            .withName("p1")
            .withParent(root)
            .build()
        p2 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d2"))
            .withName("p2")
            .withParent(root)
            .build()
        p3 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d1/d3"))
            .withName("p3")
            .withParent(p1)
            .build()
        val p3config = p3.configurations.create("p3config")
        p3config.dependencies.add(p3.dependencies.project(mutableMapOf("path" to ":p1")))
        p4 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d1/d3/d4"))
            .withName("p4")
            .withParent(p3)
            .build()
        val p4config = p4.configurations.create("p4config")
        p4config.dependencies.add(p4.dependencies.project(mutableMapOf("path" to ":p1:p3")))
        p5 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d2/d5"))
            .withName("p5")
            .withParent(p2)
            .build()
        val p5config = p5.configurations.create("p5config")
        p5config.dependencies.add(p5.dependencies.project(mutableMapOf("path" to ":p2")))
        p5config.dependencies.add(p5.dependencies.project(mutableMapOf("path" to ":p1:p3")))
        p6 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d1/d3/d6"))
            .withName("p6")
            .withParent(p3)
            .build()
        val p6config = p6.configurations.create("p6config")
        p6config.dependencies.add(p6.dependencies.project(mutableMapOf("path" to ":p2")))
        p7 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d7"))
            .withName("p7")
            .withParent(root)
            .build()
        p8 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d8"))
            .withName("cobuilt1")
            .withParent(root)
            .build()
        p9 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d9"))
            .withName("cobuilt2")
            .withParent(root)
            .build()
        p10 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("d10"))
            .withName("benchmark")
            .withParent(root)
            .build()

        // UI modules
        p12 = ProjectBuilder.builder()
            .withProjectDir(tmpDir2.resolve("compose"))
            .withName("compose")
            .withParent(root2)
            .build()
        // The existence of this project is a test for the benchmark use case. It is picked up by
        p13 = ProjectBuilder.builder() // allProjects in ui, even though it is in the root1 dir
            .withProjectDir(tmpDir.resolve("d10")) // and is symlinked as p10
            .withName("benchmark")
            .withParent(root2)
            .build()

        // The quixotic project is a valid but highly unusual project set up consisting of common
        // modules and project flavours all (effectively) at the same level as the root project
        // directory and dependencies between them.
        p16 = ProjectBuilder.builder()
            .withProjectDir(tmpDir3.resolve("d14/d16"))
            .withName("p16")
            .withParent(root3)
            .build()
        p17 = ProjectBuilder.builder()
            .withProjectDir(tmpDir3.resolve("d15/d17"))
            .withName("p17")
            .withParent(root3)
            .build()
        val p17config = p17.configurations.create("p17config")
        p17config.dependencies.add(p17.dependencies.project(mutableMapOf("path" to ":p16")))
        p18 = ProjectBuilder.builder()
            .withProjectDir(tmpDir3.resolve("d15/d18"))
            .withName("p18")
            .withParent(root3)
            .build()
        val p18config = p18.configurations.create("p18config")
        p18config.dependencies.add(p18.dependencies.project(mutableMapOf("path" to ":p16")))
        p19 = ProjectBuilder.builder()
            .withProjectDir(tmpDir3.resolve("d15/d19"))
            .withName("p19")
            .withParent(root3)
            .build()
        val p19config = p19.configurations.create("p19config")
        p19config.dependencies.add(p19.dependencies.project(mutableMapOf("path" to ":p18")))

        rootProjectGraph = ProjectGraph(root, null)
        rootDependencyTracker = DependencyTracker(root, null)
        root2ProjectGraph = ProjectGraph(root2, null)
        root2DependencyTracker = DependencyTracker(root2, null)
        root3ProjectGraph = ProjectGraph(root3, null)
        root3DependencyTracker = DependencyTracker(root3, null)

        affectedModuleConfiguration = AffectedModuleConfiguration().also {
            it.baseDir = tmpDir.absolutePath
            it.pathsAffectingAllModules = pathsAffectingAllModules
        }
    }

    @Test
    fun noChangeCLs() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                    p7.projectPath,
                    p8.projectPath,
                    p9.projectPath,
                    p10.projectPath,
                )
            )
        )
    }

    @Test
    fun noChangeCLsOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                    p7.projectPath,
                    p8.projectPath,
                    p9.projectPath,
                    p10.projectPath,
                )
            )
        )
    }

    @Test
    fun noChangeCLsOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInOne() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                )
            )
        )
    }

    @Test
    fun noChangeSkipAll() {
        affectedModuleConfiguration = affectedModuleConfiguration.also {
            it.buildAllWhenNoProjectsChanged = false
        }
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                emptySet()
            )
        )
    }

    @Test
    fun changeInOneOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInOneOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1.projectPath)
            )
        )
    }

    @Test
    fun changeInTwo() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d1", "foo.java"),
                    convertToFilePath("d2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInTwoOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d1", "foo.java"),
                    convertToFilePath("d2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInTwoOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d1", "foo.java"),
                    convertToFilePath("d2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInRootOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf("foo.java"),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInRootOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf("foo.java"),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInRootAndSubproject_onlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf("foo.java", convertToFilePath("d7", "bar.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7.projectPath)
            )
        )
    }

    @Test
    fun changeInUi_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12.projectPath)
            )
        )
    }

    @Test
    fun changeInUiOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12.projectPath)
            )
        )
    }

    @Test
    fun changeInUiOnlyDependent_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInUi_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInUiOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInUiOnlyDependent_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInNormal_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "d8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p8.projectPath)
            )
        )
    }

    @Test
    fun changeInNormalOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "d8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p8.projectPath)
            )
        )
    }

    @Test
    fun changeInNormalOnlyDependent_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "d8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInNormal_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "d8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInBoth_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12.projectPath)
            )
        )
    }

    @Test
    fun changeInBothOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12.projectPath)
            )
        )
    }

    @Test
    fun changeInBothOnlyDependent_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf() // a change to a project in the normal build doesn't affect the ui build
            )
        ) // and compose is in changed and so excluded from dependent
    }

    @Test
    fun changeInBoth_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7.projectPath) // a change in compose is known not to matter to the normal build
            )
        )
    }

    @Test
    fun changeInBothOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7.projectPath)
            )
        )
    }

    @Test
    fun changeInBothOnlyDependent_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf() // a change in compose is known not to matter to the normal build
            )
        ) // and p7 is in changed and so not in dependent
    }

    @Test
    fun changeInNormalRoot_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("..", "gradle.properties")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf() // a change in androidx root normally doesn't affect the ui build
            )
        ) // unless otherwise specified (e.g. gradlew)
    }

    @Test
    fun changeInUiRoot_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("gradle.properties")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                emptySet()
            )
        ) // a change in ui/root affects all ui projects
    }

    @Test
    fun changeInBuildSrc_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                    p7.projectPath,
                    p8.projectPath,
                    p9.projectPath,
                    p10.projectPath,
                )
            )
        ) // a change to buildSrc affects everything in both builds
    }

    @Test
    fun changeInBuildSrc_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration

        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p12.projectPath,
                    p13.projectPath,
                ) // a change to buildSrc affects everything in both builds
            )
        )
    }

    @Test
    fun changeInUiGradlew_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("ui", "gradlew")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf() // a change to ui gradlew affects only the ui build
            )
        )
    }

    @Test
    fun changeInNormalGradlew_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("android", "gradlew")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p12.projectPath,
                    p13.projectPath,
                ) // a change to root gradlew affects everything in both builds
            )
        )
    }

    @Test
    fun changeInDevelopment_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.sh")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root2),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p12.projectPath,
                    p13.projectPath,
                ) // a change to development affects everything in both builds
            )
        )
    }

    @Test
    fun changeInTools_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root2ProjectGraph,
            dependencyTracker = root2DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath(
                        "tools",
                        "android",
                        "buildSrc",
                        "sample.thing?"
                    )
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p12.projectPath,
                    p13.projectPath,
                ) // not sure what this folder is for, but it affects all of both?
            )
        )
    }

    @Test
    fun projectSubset_changed() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1.projectPath)
            )
        )
        // Test changed
        MatcherAssert.assertThat(
            detector.getSubset(p1),
            CoreMatchers.`is`(
                ProjectSubset.CHANGED_PROJECTS
            )
        )
        // Test dependent
        MatcherAssert.assertThat(
            detector.getSubset(p3),
            CoreMatchers.`is`(
                ProjectSubset.DEPENDENT_PROJECTS
            )
        )
        // Random unrelated project should return none
        MatcherAssert.assertThat(
            detector.getSubset(p12),
            CoreMatchers.`is`(
                ProjectSubset.NONE
            )
        )
    }

    @Test
    fun projectSubset_dependent() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                )
            )
        )
        // Test changed
        MatcherAssert.assertThat(
            detector.getSubset(p1),
            CoreMatchers.`is`(
                ProjectSubset.CHANGED_PROJECTS
            )
        )
        // Test dependent
        MatcherAssert.assertThat(
            detector.getSubset(p3),
            CoreMatchers.`is`(
                ProjectSubset.DEPENDENT_PROJECTS
            )
        )
        // Random unrelated project should return none
        MatcherAssert.assertThat(
            detector.getSubset(p12),
            CoreMatchers.`is`(
                ProjectSubset.NONE
            )
        )
    }

    @Test
    fun projectSubset_all() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                )
            )
        )
        // Test changed
        MatcherAssert.assertThat(
            detector.getSubset(p1),
            CoreMatchers.`is`(
                ProjectSubset.CHANGED_PROJECTS
            )
        )
        // Test dependent
        MatcherAssert.assertThat(
            detector.getSubset(p3),
            CoreMatchers.`is`(
                ProjectSubset.DEPENDENT_PROJECTS
            )
        )
        // Random unrelated project should return none
        MatcherAssert.assertThat(
            detector.getSubset(p12),
            CoreMatchers.`is`(
                ProjectSubset.NONE
            )
        )
    }

    @Test
    fun noChangeCLs_quixotic() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p16.projectPath,
                    p17.projectPath,
                    p18.projectPath,
                    p19.projectPath,
                )
            )
        )
    }

    @Test
    fun noChangeCLsOnlyDependent_quixotic() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p16.projectPath,
                    p17.projectPath,
                    p18.projectPath,
                    p19.projectPath,
                )
            )
        )
    }

    @Test
    fun noChangeCLsOnlyChanged_quixotic() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = emptyList(),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun changeInOne_quixotic_main_module() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d14", "d16", "foo.java")),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p16.projectPath,
                    p17.projectPath,
                    p18.projectPath,
                    p19.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInOne_quixotic_common_module_with_a_dependency() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d15", "d18", "foo.java")),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p18.projectPath,
                    p19.projectPath,
                )
            )
        )
    }

    @Test
    fun changeInOne_quixotic_common_module_without_a_dependency() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = root3ProjectGraph,
            dependencyTracker = root3DependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d15", "d19", "foo.java")),
                tmpFolder = root3.projectDir
            ).findChangedFiles(root3),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p19.projectPath)
            )
        )
    }

    @Test
    fun `GIVEN affected module configuration WHEN invalid path THEN throw exception`() {
        // GIVEN
        val config = AffectedModuleConfiguration().also {
            it.baseDir = tmpFolder.root.absolutePath
        }

        // WHEN
        config.pathsAffectingAllModules = setOf("invalid")
        try {
            config.pathsAffectingAllModules.forEach {
                // no op
            }
            fail("Invalid state, should have thrown exception")
        } catch (e: IllegalArgumentException) {
            // THEN
            Truth.assertThat("Could not find expected path in pathsAffectingAllModules: invalid")
                .isEqualTo(e.message)
        }
    }

    @Test
    fun `GIVEN affected module configuration WHEN valid paths THEN return paths`() {
        // GIVEN
        val config = AffectedModuleConfiguration().also {
            it.baseDir = tmpFolder.root.absolutePath
        }

        // WHEN
        config.pathsAffectingAllModules = pathsAffectingAllModules
        val result = config.pathsAffectingAllModules

        // THEN
        Truth.assertThat(result).isEqualTo(pathsAffectingAllModules)
    }

    @Test
    fun `GIVEN all affected modules WHEN modules parameter is passed THEN modules parameter is observed`() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = setOf(":p1"),
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p4.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p5.projectPath)).isFalse()
    }

    @Test
    fun `GIVEN all affected modules WHEN modules parameter is empty THEN no affected modules `() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = emptySet(),
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p3.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p4.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p5.projectPath)).isFalse()
    }

    @Test
    fun `GIVEN all affected modules WHEN modules parameter is null THEN all affected modules are returned `() {
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p3.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p4.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p5.projectPath)).isTrue()
    }

    @Test
    fun `GIVEN module is in excludedModules configuration WHEN shouldInclude THEN excluded module false AND dependent modules true`() {
        affectedModuleConfiguration = affectedModuleConfiguration.also {
            it.excludedModules = setOf("p1")
        }
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(convertToFilePath("d1", "foo.java")),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p4.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p5.projectPath)).isTrue()
    }

    @Test
    fun `GIVEN regex is in excludedModules configuration WHEN shouldInclude THEN excluded module false AND dependent modules true`() {
        affectedModuleConfiguration = affectedModuleConfiguration.also {
            it.excludedModules = setOf(":p1:p3:[a-zA-Z0-9:]+")
        }
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(
                    convertToFilePath("d1/d3", "foo.java"),
                    convertToFilePath("d1/d3/d4", "foo.java"),
                    convertToFilePath("d2/d5", "foo.java"),
                    convertToFilePath("d1/d3/d6", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p3.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p4.projectPath)).isFalse()
        Truth.assertThat(detector.shouldInclude(p5.projectPath)).isTrue()
        Truth.assertThat(detector.shouldInclude(p6.projectPath)).isFalse()
    }

    @Test
    fun `GIVEN a file that effects all changes has a change WHEN projectSubset is CHANGED_PROJECTS THEN all modules should be in this`() {
        val changedFile = convertToFilePath("android", "gradle", "test.java")

        affectedModuleConfiguration = affectedModuleConfiguration.also {
            it.pathsAffectingAllModules.toMutableSet().add(changedFile)
        }
        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(changedFile),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(
                    p1.projectPath,
                    p2.projectPath,
                    p3.projectPath,
                    p4.projectPath,
                    p5.projectPath,
                    p6.projectPath,
                    p7.projectPath,
                    p8.projectPath,
                    p9.projectPath,
                    p10.projectPath,
                )
            )
        )
    }

    @Test
    fun `GIVEN a file that does not affect all projects has a change WHEN projectSubset is CHANGED_PROJECTS THEN affected projects is empty`() {
        val changedFile = convertToFilePath("android", "notgradle", "test.java")

        val detector = AffectedModuleDetectorImpl(
            projectGraph = rootProjectGraph,
            dependencyTracker = rootDependencyTracker,
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(changedFile),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf()
            )
        )
    }

    @Test
    fun `GIVEN submodule with projects WHEN submodule changes THEN all projects under submodule are affected`() {
        // Create submodule directory with a project inside
        val submodulePath = convertToFilePath("libs", "my-submodule")
        val submoduleDir = File(tmpFolder.root, submodulePath)
        submoduleDir.mkdirs()

        // Create .gitmodules file
        File(tmpFolder.root, ".gitmodules").writeText("""
            [submodule "libs/my-submodule"]
                path = libs/my-submodule
                url = https://github.com/example/repo.git
        """.trimIndent())

        // Create a project inside the submodule
        val submoduleProject = ProjectBuilder.builder()
            .withProjectDir(submoduleDir.resolve("module-a"))
            .withName("module-a")
            .withParent(root)
            .build()

        val submoduleProjectGraph = ProjectGraph(root, null)

        val detector = AffectedModuleDetectorImpl(
            projectGraph = submoduleProjectGraph,
            dependencyTracker = DependencyTracker(root, null),
            logger = logger.toLogger(),
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            modules = null,
            changedFilesProvider = MockGitClient(
                changedFiles = listOf(submodulePath),
                tmpFolder = tmpFolder.root
            ).findChangedFiles(root),
            gitRoot = tmpFolder.root,
            config = affectedModuleConfiguration
        )

        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.hasItem(submoduleProject.projectPath)
        )
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockGitClient(
        val changedFiles: List<String>,
        val tmpFolder: File
    ) : GitClient {

        override fun findChangedFiles(
            project: Project,
        ): Provider<List<String>> = project.provider { changedFiles }

        override fun getGitRoot(): File {
            return tmpFolder
        }
    }
}
