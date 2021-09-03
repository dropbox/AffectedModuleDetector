import com.dropbox.affectedmoduledetector.Dependencies

/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
plugins {
    kotlin("jvm") version Dependencies.Versions.KOTLIN_VERSION
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(Dependencies.Libs.AFFECTED_MODULE_DETECTOR)
    testImplementation(Dependencies.Libs.JUNIT)
    testImplementation(Dependencies.Libs.MOCKITO_KOTLIN)
    testImplementation(Dependencies.Libs.TRUTH)
}
