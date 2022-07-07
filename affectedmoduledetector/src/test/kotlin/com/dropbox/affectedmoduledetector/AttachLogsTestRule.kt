package com.dropbox.affectedmoduledetector

import com.nhaarman.mockito_kotlin.mock
import org.gradle.api.provider.Provider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Special rule for dependency detector tests that will attach logs to a failure.
 */
class AttachLogsTestRule : TestRule {
    internal val logger = mock<ToStringLogger> {  }
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    throw Exception(
                        """
                                test failed with msg: ${t.message}
                                logs:
                                ${logger.buildString()}
                            """.trimIndent(),
                        t
                    )
                }
            }
        }
    }
}
