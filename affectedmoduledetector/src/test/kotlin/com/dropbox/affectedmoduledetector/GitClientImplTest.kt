package com.dropbox.affectedmoduledetector

import java.io.File
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.dropbox.affectedmoduledetector.GitClientImpl.Companion.CHANGED_FILES_CMD_PREFIX

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
    private val client = GitClientImpl(
        workingDir = workingDir,
        logger = logger,
        commandRunner = commandRunner
    )


    @Test
    fun findChangesSince() {
        var changes = listOf(
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX HEAD..mySha",
                changes.joinToString(System.lineSeparator())
        )
        assertEquals(
                changes,
                client.findChangedFilesSince(sha = "mySha", includeUncommitted = true))
    }

    @Test
    fun findChangesSince_empty() {
        assertEquals(
                emptyList<String>(),
                client.findChangedFilesSince("foo"))
    }

    @Test
    fun findChangesSince_twoCls() {
        var changes = listOf(
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX otherSha mySha",
                changes.joinToString(System.lineSeparator())
        )
        assertEquals(
                changes,
                client.findChangedFilesSince(
                        sha = "mySha",
                        top = "otherSha",
                        includeUncommitted = false))
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockCommandRunner(val logger: ToStringLogger) : GitClient.CommandRunner {
        private val replies = mutableMapOf<String, List<String>>()

        fun addReply(command: String, response: String) {
            logger.info("add reply. cmd: $command response: $response")
            replies[command] = response.split(System.lineSeparator())
        }

        override fun execute(command: String): String {
            return replies.getOrDefault(command, emptyList())
                .joinToString(System.lineSeparator()).also {
                    logger.info("cmd: $command response: $it")
            }
        }

        override fun executeAndParse(command: String): List<String> {
            return replies.getOrDefault(command, emptyList()).also {
                logger.info("cmd: $command response: $it")
            }
        }
    }
}
