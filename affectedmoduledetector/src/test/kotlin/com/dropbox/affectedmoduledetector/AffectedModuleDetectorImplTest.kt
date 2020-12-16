package com.dropbox.affectedmoduledetector

import com.google.common.truth.Truth
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
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
    private val logger = attachLogsRule.logger
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()
    @Rule
    @JvmField
    val tmpFolder2 = TemporaryFolder()

    private lateinit var root: Project
    private lateinit var root2: Project
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
    private val pathsAffectingAllModules = setOf(
        "tools/android/buildSrc",
        "android/gradlew",
        "android/gradle",
        "dbx/core/api/"
    )
    private lateinit var affectedModuleConfiguration: AffectedModuleConfiguration

    @Before
    fun init() {
        val tmpDir = tmpFolder.root
        val tmpDir2 = tmpFolder2.root

        pathsAffectingAllModules.forEach {
            File(tmpDir, it).mkdirs()
        }

        /*

        Dummy project file tree:
               "library modules"           "UI modules"
               root -----------------       root2
              / |  \     |   |   |   |       /  |  \
            p1  p7  p2  p8   p9 p10  p11    p12 p13 p14
           /         \
          p3          p5
         /  \
       p4   p6

        Dependency forest:

            p1    p2    p7 p8  p9 p10 p11     p12 p14
           /  \  /  \               \
          p3   p5   p6               p13
         /
        p4

         */

        root = ProjectBuilder.builder()
            .withProjectDir(tmpDir)
            .withName("root")
            .build()
        // Project Graph expects supportRootFolder.
        (root.properties.get("ext") as ExtraPropertiesExtension).set("supportRootFolder", tmpDir)
        root2 = ProjectBuilder.builder()
            .withProjectDir(tmpDir2)
            .withName("root2/ui")
            .build()
        // Project Graph expects supportRootFolder.
        (root2.properties.get("ext") as ExtraPropertiesExtension).set("supportRootFolder", tmpDir2)
        p1 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1"))
            .withName("p1")
            .withParent(root)
            .build()
        p2 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p2"))
            .withName("p2")
            .withParent(root)
            .build()
        p3 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3"))
            .withName("p3")
            .withParent(p1)
            .build()
        val p3config = p3.configurations.create("p3config")
        p3config.dependencies.add(p3.dependencies.project(mutableMapOf("path" to ":p1")))
        p4 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3:p4"))
            .withName("p4")
            .withParent(p3)
            .build()
        val p4config = p4.configurations.create("p4config")
        p4config.dependencies.add(p4.dependencies.project(mutableMapOf("path" to ":p1:p3")))
        p5 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p2:p5"))
            .withName("p5")
            .withParent(p2)
            .build()
        val p5config = p5.configurations.create("p5config")
        p5config.dependencies.add(p5.dependencies.project(mutableMapOf("path" to ":p2")))
        p5config.dependencies.add(p5.dependencies.project(mutableMapOf("path" to ":p1:p3")))
        p6 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p1:p3:p6"))
            .withName("p6")
            .withParent(p3)
            .build()
        val p6config = p6.configurations.create("p6config")
        p6config.dependencies.add(p6.dependencies.project(mutableMapOf("path" to ":p2")))
        p7 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p7"))
            .withName("p7")
            .withParent(root)
            .build()
        p8 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p8"))
            .withName("cobuilt1")
            .withParent(root)
            .build()
        p9 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p9"))
            .withName("cobuilt2")
            .withParent(root)
            .build()
        p10 = ProjectBuilder.builder()
            .withProjectDir(tmpDir.resolve("p10"))
            .withName("benchmark")
            .withParent(root)
            .build()
        p12 = ProjectBuilder.builder()
            .withProjectDir(tmpDir2.resolve("compose"))
            .withName("compose")
            .withParent(root2)
            .build()
        // The existence of this project is a test for the benchmark use case. It is picked up by
        p13 = ProjectBuilder.builder() // allProjects in ui, even though it is in the root1 dir
            .withProjectDir(tmpDir.resolve("p10")) // and is symlinked as p10
            .withName("benchmark")
            .withParent(root2)
            .build()

        affectedModuleConfiguration = AffectedModuleConfiguration().also {
            it.baseDir = tmpDir.absolutePath
            it.pathsAffectingAllModules = pathsAffectingAllModules
        }
    }

    @Test
    fun noChangeCLs() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
            )
        )
    }

    @Test
    fun noChangeCLsOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
            )
        )
    }

    @Test
    fun noChangeCLsOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = emptyList(),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p3, p4, p5)
            )
        )
    }

    @Test
    fun changeInOneOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p3, p4, p5)
            )
        )
    }

    @Test
    fun changeInOneOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1)
            )
        )
    }

    @Test
    fun changeInTwo() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p1", "foo.java"),
                    convertToFilePath("p2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6)
            )
        )
    }

    @Test
    fun changeInTwoOnlyDependent() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p1", "foo.java"),
                    convertToFilePath("p2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p3, p4, p5, p6)
            )
        )
    }

    @Test
    fun changeInTwoOnlyChanged() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p1", "foo.java"),
                    convertToFilePath("p2", "bar.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p2)
            )
        )
    }

    @Test
    fun changeInRootOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf("foo.java"),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf("foo.java"),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf("foo.java", convertToFilePath("p7", "bar.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7)
            )
        )
    }

    @Test
    fun changeInUi_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12)
            )
        )
    }

    @Test
    fun changeInUiOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12)
            )
        )
    }

    @Test
    fun changeInUiOnlyDependent_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "compose", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "p8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p8)
            )
        )
    }

    @Test
    fun changeInNormalOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "p8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p8)
            )
        )
    }
    @Test
    fun changeInNormalOnlyDependent_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "p8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath(
                        "p8", "foo.java"
                    )
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12)
            )
        )
    }

    @Test
    fun changeInBothOnlyChanged_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12)
            )
        )
    }

    @Test
    fun changeInBothOnlyDependent_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7) // a change in compose is known not to matter to the normal build
            )
        )
    }

    @Test
    fun changeInBothOnlyChanged_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p7)
            )
        )
    }

    @Test
    fun changeInBothOnlyDependent_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("p7", "foo.java"),
                    convertToFilePath("compose", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("..", "gradle.properties")
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("gradle.properties")
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)
            )
        ) // a change to buildSrc affects everything in both builds
    }

    @Test
    fun changeInBuildSrc_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.java")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration

        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12, p13) // a change to buildSrc affects everything in both builds
            )
        )
    }

    @Test
    fun changeInUiGradlew_normalBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("ui", "gradlew")
                ),
                tmpFolder = tmpFolder.root
            ),
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
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("android", "gradlew")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12, p13) // a change to root gradlew affects everything in both builds
            )
        )
    }

    @Test
    fun changeInDevelopment_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(
                    convertToFilePath("tools", "android", "buildSrc", "foo.sh")
                ),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12, p13) // a change to development affects everything in both builds
            )
        )
    }

    @Test
    fun changeInTools_uiBuild() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root2,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("tools", "android", "buildSrc", "sample.thing?")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p12, p13) // not sure what this folder is for, but it affects all of both?
            )
        )
    }

    @Test
    fun projectSubset_changed() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.CHANGED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1)
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.DEPENDENT_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p3, p4, p5)
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        // Verify expectations on affected projects
        MatcherAssert.assertThat(
            detector.affectedProjects,
            CoreMatchers.`is`(
                setOf(p1, p3, p4, p5)
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
            Truth.assertThat("Could not find expected path in pathsAffectingAllModules: invalid").isEqualTo(e.message)
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
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = setOf(":p1"),
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1)).isTrue()
        Truth.assertThat(detector.shouldInclude(p4)).isFalse()
        Truth.assertThat(detector.shouldInclude(p5)).isFalse()
    }

    @Test
    fun `GIVEN all affected modules WHEN modules parameter is empty THEN no affected modules `() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = emptySet(),
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1)).isFalse()
        Truth.assertThat(detector.shouldInclude(p3)).isFalse()
        Truth.assertThat(detector.shouldInclude(p4)).isFalse()
        Truth.assertThat(detector.shouldInclude(p5)).isFalse()
    }

    @Test
    fun `GIVEN all affected modules WHEN modules parameter is null THEN all affected modules are returned `() {
        val detector = AffectedModuleDetectorImpl(
            rootProject = root,
            logger = logger,
            ignoreUnknownProjects = false,
            projectSubset = ProjectSubset.ALL_AFFECTED_PROJECTS,
            modules = null,
            injectedGitClient = MockGitClient(
                lastMergeSha = "foo",
                changedFiles = listOf(convertToFilePath("p1", "foo.java")),
                tmpFolder = tmpFolder.root
            ),
            config = affectedModuleConfiguration
        )
        Truth.assertThat(detector.shouldInclude(p1)).isTrue()
        Truth.assertThat(detector.shouldInclude(p3)).isTrue()
        Truth.assertThat(detector.shouldInclude(p4)).isTrue()
        Truth.assertThat(detector.shouldInclude(p5)).isTrue()
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockGitClient(
        val lastMergeSha: String?,
        val changedFiles: List<String>,
        val tmpFolder: File
    ) : GitClient {

        override fun findChangedFilesSince(
            sha: String,
            top: String,
            includeUncommitted: Boolean
        ) = changedFiles

        override fun findPreviousCommitSha() = lastMergeSha

        override fun getGitRoot(): File {
            return tmpFolder
        }
    }
}
