package com.dropbox.affectedmoduledetector

import java.io.File
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.dropbox.affectedmoduledetector.GitClientImpl.Companion.CHANGED_FILES_CMD_PREFIX
import com.dropbox.affectedmoduledetector.mocks.MockCommandRunner
import com.dropbox.affectedmoduledetector.mocks.MockCommitShaProvider

@RunWith(JUnit4::class)
class GitClientImplTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    /** The [GitClientImpl.workingDir] uses `System.getProperty("user.dir")` because the working
     * directory passed to the [GitClientImpl] constructor needs to contain the a .git
     * directory somewhere in the parent directory tree.  @see [GitClientImpl]
     */
    private val workingDir = File(System.getProperty("user.dir")).parentFile
    private val commitShaProvider = MockCommitShaProvider()
    private val client = GitClientImpl(
        workingDir = workingDir,
        logger = logger,
        commandRunner = commandRunner,
        commitShaProvider = commitShaProvider,
        ignoredFiles = setOf(".*\\.ignore", ".*IGNORED_FILE")
    )

    @Test
    fun givenChangedFiles_whenFindChangedFilesIncludeUncommitted_thenReturnChanges() {
        val changes = listOf(
                convertToFilePath("d", "e", ".ignoredNot"),
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        val changesWithIgnores = listOf(
                convertToFilePath("a", "b", "anything.ignore"),
                convertToFilePath("d", "e", ".ignore"),
                convertToFilePath("d", "e", "IGNORED_FILE")
        ) + changes
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX mySha",
            changesWithIgnores.joinToString(System.lineSeparator())
        )
        commitShaProvider.addReply("mySha")

        assertEquals(
                changes,
                client.findChangedFiles(includeUncommitted = true))
    }

    @Test
    fun findChangesSince_empty() {
        commitShaProvider.addReply("mySha")
        assertEquals(
                emptyList<String>(),
                client.findChangedFiles()
        )
    }

    @Test
    fun findChangesSince_twoCls() {
        val changes = listOf(
                convertToFilePath("d", "e", ".ignoredNot"),
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        val changesWithIgnores = listOf(
                convertToFilePath("a", "b", "anything.ignore"),
                convertToFilePath("d", "e", ".ignore"),
                convertToFilePath("d", "e", "IGNORED_FILE")
        ) + changes
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX otherSha..mySha",
            changesWithIgnores.joinToString(System.lineSeparator())
        )
        commitShaProvider.addReply("mySha")
        assertEquals(
                changes,
                client.findChangedFiles(
                    top = "otherSha",
                    includeUncommitted = false
                ))
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }
}
