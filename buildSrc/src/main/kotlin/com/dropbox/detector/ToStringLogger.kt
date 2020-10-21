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

package com.dropbox.detector

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLogger
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.gradle.internal.time.Clock

/**
 * Gradle logger that logs to a string.
 */
class ToStringLogger(
    private val stringBuilder: StringBuilder = StringBuilder()
) : OutputEventListenerBackedLogger(
        "amd",
        OutputEventListenerBackedLoggerContext(
                Clock {
                    System.currentTimeMillis()
                }
        ).also {
            it.level = LogLevel.DEBUG
            it.setOutputEventListener {
                stringBuilder.appendln(it.toString())
            }
        },
        Clock {
            System.currentTimeMillis()
        }
) {
    /**
     * Returns the current log.
     */
    fun buildString() = stringBuilder.toString()

    companion object {
        fun createWithLifecycle(
            gradle: Gradle,
            onComplete: (String) -> Unit
        ): Logger {
            val logger = ToStringLogger()
            gradle.addBuildListener(object : BuildAdapter() {
                override fun buildFinished(result: BuildResult) {
                    onComplete(logger.buildString())
                }
            })
            return logger
        }
    }
}
