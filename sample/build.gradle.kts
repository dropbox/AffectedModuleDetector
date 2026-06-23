import com.dropbox.affectedmoduledetector.AffectedModuleConfiguration

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.affected.module.detector)
    id("affected-tests-plugin") // custom plugin based on AMD
    id("affected-tasks-plugin") // custom plugin based on AMD
}

affectedModuleDetector {
    baseDir = project.rootDir.toString()
    pathsAffectingAllModules = setOf(
        "build_configuration/",
    )
    specifiedBranch = "origin/main"
    compareFrom = "SpecifiedBranchCommitMergeBase"
    customTasks = setOf(
        AffectedModuleConfiguration.CustomTask(
            "runDetektByImpact",
            "detekt",
            "Run static analysis tool by Impact analysis",
        ),
    )
    logFolder = project.rootDir.toString()
    excludedModules = setOf(
        "sample-util",
    )
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
