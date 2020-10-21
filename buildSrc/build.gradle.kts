/*
 * Copyright (c) 2020, Dropbox, Inc. All rights reserved.
 */
plugins {
    kotlin("jvm") version "1.4.10"
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
