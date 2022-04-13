# Affected Module Detector

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.dropbox.affectedmoduledetector/affectedmoduledetector/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.dropbox.affectedmoduledetector/affectedmoduledetector/)

[![Build Status](https://travis-ci.org/dropbox/AffectedModuleDetector.svg?branch=main)](https://travis-ci.org/dropbox/AffectedModuleDetector)

[![codecov](https://codecov.io/gh/dropbox/AffectedModuleDetector/branch/main/graph/badge.svg)](https://codecov.io/gh/dropbox/AffectedModuleDetector)

A Gradle Plugin to determine which modules were affected by a set of files in a commit.  One use case for this plugin is for developers who would like to only run tests in modules which have changed in a given commit.

## Overview

The AffectedModuleDetector will look at the last commit and determine which files have changed, it will then build a dependency graph of all the modules in the project.   The detector exposes a set of APIs which can be used to determine whether a module was considered affected.

### Git

The module detector assumes that it is being applied to a project stored in git and a git client is present on the system.  It will query the last commit on the current branch to determine the list of files changed.

### Dependency Tracker

The tracker will evaluate the project and find all modules and their dependencies for all configurations.

### Affected Module Detector

The detector allows for three options for affected modules:
 - **Changed Projects**: These are projects which had files changed within them – enabled with `-Paffected_module_detector.changedProjects`)
 - **Dependent Projects**: These are projects which are dependent on projects which had changes within them – enabled with `-Paffected_module_detector.dependentProjects`)
 - **All Affected Projects**:  This is the union of Changed Projects and Dependent Projects (this is the default configuration) 

 These options can be useful depending on how many tests your project has and where in the integration cycle you would like to run them.  For example, Changed Projects may be a good options when initially sending a Pull Requests, and All Affected Projects may be useful to use when a developer merges their pull request.

The detector exposes APIs which will be helpful for your plugin to use.  In particular, it exposes:
 - AffectedModuleDetector.configureTaskGuard - This will apply an `onlyIf` guard on your task and can be called either during configuration or execution
 - AffectedModuleDetector.isProjectAffected - This will return a boolean if the project has been affected. It can only be called after the project has been configured.


In the example below, we're showing a hypothetical project graph and what projects would be considered affected if the All Affected Projects option was used and a change was made in the `:networking` module.
<img src="./dependency_graph.png">

## Installation

```groovy
// settings.gradle(.kts)
pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

// root build.gradle(.kts)
plugins {
  id("com.dropbox.affectedmoduledetector") version "<latest-version>"
}
```

Note that the plugin is currently published to Maven Central, so you need to add it to the repositories list in the `pluginsManagement` block.

Alternatively, it can be consumed via manual buildscript dependency + plugin application.

Apply the project to the root `build.gradle`:
```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "com.dropbox.affectedmoduledetector:affectedmoduledetector:<LATEST_VERSION>"
  }
}
//rootproject
apply plugin: "com.dropbox.affectedmoduledetector"
```

If you want to develop a plugin using the APIs, add this to your `buildSrc`'s `dependencies` list:
```
implementation("com.dropbox.affectedmoduledetector:affectedmoduledetector:<LATEST_VERSION>")
```

## Configuration

You can specify the configuration block for the detector in the root project:
```groovy
affectedModuleDetector {
    baseDir = "${project.rootDir}"
    pathsAffectingAllModules = [
            "buildSrc/"
    ]
    logFilename = "output.log"
    logFolder = "${project.rootDir}/output"
    compareFrom = "PreviousCommit" //default is PreviousCommit
    excludedModules = [
        "sample-util"
    ]
    includeUncommitted = true
    top = "HEAD"
}
```

 - `baseDir`: The root directory for all of the `pathsAffectingAllModules`. Used to validate the paths exist.
 - `pathsAffectingAllModules`: Paths to files or folders which if changed will trigger all modules to be considered affected
 - `logFilename`: A filename for the output detector to use
 - `logFolder`: A folder to output the log file in
 - `specifiedBranch`: A branch to specify changes against. Must be used in combination with configuration `compareFrom = "SpecifiedBranchCommit"` 
 - `compareFrom`: A commit to compare the branch changes against. Can be either:
    - PreviousCommit: compare against the previous commit
    - ForkCommit: compare against the commit the branch was forked from
    - SpecifiedBranchCommit: specify the branch to compare changes against using the `specifiedBranch` configuration before the `compareFrom` configuration
 - `excludedModules`: A list of modules that will be excluded from the build process
 - `includeUncommitted`: If uncommitted files should be considered affected
 - `top`: The top of the git log to use. Must be used in combination with configuration `includeUncommitted = false`



 By default, the Detector will look for `assembleAndroidDebugTest`, `connectedAndroidDebugTest`, and `testDebug`.  Modules can specify a configuration block to specify which variant tests to run:
 ```groovy
 affectedTestConfiguration {
    assembleAndroidTestTask = "assembleAndroidReleaseTest"
    runAndroidTestTask = "connectedAndroidReleaseTest"
    jvmTestTask = "testRelease"
}
```
 
 The plugin will create a few top level tasks that will assemble or run tests for only affected modules:
 * `./gradlew runAffectedUnitTests` - runs jvm tests
 * `./gradlew runAffectedAndroidTests` - runs connected tests
 * `./gradlew assembleAffectedAndroidTests` - assembles but does not run on device tests, useful when working with device labs


## Sample Usage

Running the plugin generated tasks is quite simple. By default, if `affected_module_detector.enable` is not set,
the generated tasks will run on all the modules. However, the plugin offers three different modes of operation so that it
only executes the given task on a subset of projects.

#### Running All Affected Projects (Changed Projects + Dependent Projects)

To run all the projects affected by a change, run one of the tasks while enabling the module detector.

```
./gradlew runAffectedUnitTests -Paffected_module_detector.enable
```

#### Running All Changed Projects

To run all the projects that changed, run one of the tasks (while enabling the module detector) and with `-Paffected_module_detector.changedProjects`

```
./gradlew runAffectedUnitTests -Paffected_module_detector.enable -Paffected_module_detector.changedProjects
```

#### Running All Dependent Projects

To run all the dependent projects of projects that changed, run one of the tasks (while enabling the module detector) and with `-Paffected_module_detector.dependentProjects`

```
./gradlew runAffectedUnitTests -Paffected_module_detector.enable -Paffected_module_detector.dependentProjects
```

## Using the Sample project

To run this on the sample app:

1. Publish the plugin to local maven:
```
./gradlew :affectedmoduledetector:publishToMavenLocal
```

2. Try running the following command:

```
 ./gradlew runAffectedUnitTests -Paffected_module_detector.enable
```

You should see zero tests run. Make a change within one of the modules and commit it. Rerunning the command should execute tests in that module and its dependent modules.

## Notes

Special thanks to the AndroidX for originally developing this project at https://android.googlesource.com/platform/frameworks/support/+/androidx-main/buildSrc/src/main/kotlin/androidx/build/dependencyTracker

## License

    Copyright (c) 2021 Dropbox, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
