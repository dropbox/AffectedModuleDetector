/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

gradlePlugin {
    plugins {
        register("affected-tests-plugin") {
            id = "affected-tests-plugin"
            implementationClass = "com.dropbox.affectedmoduledetector.AffectedTestsPlugin"
        }
        register("affected-tasks-plugin") {
            id = "affected-tasks-plugin"
            implementationClass = "com.dropbox.affectedmoduledetector.AffectedTasksPlugin"
        }
    }
}

dependencies {
    implementation(libs.affected.module.detector)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.google.truth)
}
