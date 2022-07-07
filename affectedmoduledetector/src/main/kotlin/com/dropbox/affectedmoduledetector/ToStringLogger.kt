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

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLogger
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.gradle.internal.operations.*
import org.gradle.internal.time.Clock
import org.gradle.invocation.DefaultGradle
import java.io.File

/**
 * Gradle logger that logs to a string.
 */
internal open class ToStringLogger(
    private val stringBuilder: Provider<ToStringLoggerBuildService>?
) : OutputEventListenerBackedLogger(
        "amd",
        OutputEventListenerBackedLoggerContext {
            System.currentTimeMillis()
        }.also {
            it.level = LogLevel.DEBUG
            it.setOutputEventListener { outputEvent ->
                stringBuilder?.get()?.parameters?.getStringBuilderProperty()?.get()?.appendLine(outputEvent.toString())
            }
        },
        Clock {
            System.currentTimeMillis()
        }
) {

    /**
     * Returns the current log.
     */
    fun buildString() = stringBuilder?.get()?.parameters?.getStringBuilderProperty()?.get()?.toString()

    @Suppress("UnstableApiUsage") // BuildService is not yet stable
    companion object {
        internal abstract class ToStringLoggerBuildService : BuildService<ToStringLoggerBuildService.ToStringLoggerBuildServiceParameters>, BuildOperationListener, AutoCloseable {
            interface ToStringLoggerBuildServiceParameters : BuildServiceParameters {
                fun getStringBuilderProperty(): Property<StringBuilder>
                fun getOutputFileProperty(): RegularFileProperty
            }

            override fun started(p0: BuildOperationDescriptor, p1: OperationStartEvent) { }

            override fun progress(p0: OperationIdentifier, p1: OperationProgressEvent) { }

            override fun finished(buildOperationDescriptor: BuildOperationDescriptor, operationFinishEvent: OperationFinishEvent) { }

            override fun close() {
                val outputFile = parameters.getOutputFileProperty().orNull?.asFile ?: return
                outputFile.appendText(parameters.getStringBuilderProperty().get().toString())
                println("Wrote dependency log to ${outputFile.absolutePath}")
            }
        }

        /**
         * Creates the [ToStringLogger]
         *
         * @param project the current project to apply to
         * @param logFilename the filename for the logs to go
         * @param logFolder the path to where the log should output. if null doesn't output
         */
        fun createWithLifecycle(
            project: Project,
            logFilename: String,
            logFolder: String? = null
        ): Logger {
            val gradle = project.gradle
            val stringBuilder = StringBuilder()
            val toStringLoggerBuildService = gradle.sharedServices.registerIfAbsent("to-string-logger-build-listener", ToStringLoggerBuildService::class.java) { service ->
                service.parameters.getStringBuilderProperty().set(stringBuilder)
                if (logFolder != null) {
                    val distDir = File(logFolder)
                    if (!distDir.exists()) {
                        distDir.mkdirs()
                    }
                    val outputFile = distDir.resolve(logFilename)
                    service.parameters.getOutputFileProperty().set(outputFile)
                }
            }
            val logger = ToStringLogger(toStringLoggerBuildService)
            (gradle as DefaultGradle).services[BuildEventListenerRegistryInternal::class.java].onOperationCompletion(toStringLoggerBuildService)
            return logger
        }
    }
}
