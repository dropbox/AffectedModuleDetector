package com.dropbox.affectedmoduledetector

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * Special rule for dependency detector tests that will attach logs to a failure.
 */
class AttachLogsTestRule() : TestRule {

    private val file: File = File.createTempFile("test", "log")

    internal val logger by lazy { FileLogger(file) }
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    file.deleteOnExit()
                    base.evaluate()
                } catch (t: Throwable) {
                    val bufferedReader = file.bufferedReader()
                    val logs = bufferedReader.use { it.readText() }
                    throw Exception(
                        """
                                test failed with msg: ${t.message}
                                logs:
                                $logs
                        """.trimIndent(),
                        t
                    )
                }
            }
        }
    }
}
