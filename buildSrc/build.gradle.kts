/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
plugins {
    kotlin("jvm") version "1.4.10"
    id("com.gradle.plugin-publish") version "0.12.0"
    `java-gradle-plugin`
}

repositories {
    google()
    jcenter()
}

dependencies {
    testImplementation("junit:junit:4.13.1")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.4.10")
    testImplementation("com.google.truth:truth:1.0.1")
}

gradlePlugin {
    plugins {
        create("affectedModuleDetectorPlugin") {
            id = "com.dropbox.detector"
            implementationClass = "com.dropbox.detector.AffectedModuleDetectorPlugin"
        }
    }
}

version = "0.1.0-SNAPSHOT"

pluginBundle {
    website = "https://github.com/Dropbox/AffectedModuleDetector"
    vcsUrl = "https://github.com/Dropbox/AffectedModuleDetector"

    description = "A Gradle Plugin to determine which modules were affected in a commit or branch."

    (plugins) {

        "affectedModuleDetectorPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Affected Module Detector"
            tags = listOf("module", "git", "detector", "dependency")
        }
    }
}