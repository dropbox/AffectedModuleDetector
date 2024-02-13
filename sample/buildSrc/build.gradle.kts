/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
plugins {
    kotlin("jvm") version "1.6.10"
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.dropbox.affectedmoduledetector:affectedmoduledetector:0.3.1-SNAPSHOT")
    testImplementation("junit:junit:4.13.1")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("com.google.truth:truth:1.0.1")
}