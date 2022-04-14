package com.dropbox.affectedmoduledetector

internal enum class InternalTaskType(
    override val commandByImpact: String,
    override val originalGradleCommand: String,
    override val taskDescription: String
): AffectedModuleTaskType {

    ANDROID_TEST(
        commandByImpact = "runAffectedAndroidTests",
        originalGradleCommand = "connectedDebugAndroidTest",
        taskDescription = "Runs all affected Android Tests. Requires a connected device."
    ),

    ASSEMBLE_ANDROID_TEST(
        commandByImpact = "assembleAffectedAndroidTests",
        originalGradleCommand = "assembleDebugAndroidTest",
        taskDescription = "Assembles all affected Android Tests. Useful when working with device labs."
    ),

    ANDROID_JVM_TEST(
        commandByImpact = "runAffectedUnitTests",
        originalGradleCommand = "testDebugUnitTest",
        taskDescription = "Runs all affected unit tests."
    ),

    JVM_TEST(
        commandByImpact = "runAffectedUnitTests",
        originalGradleCommand = "test",
        taskDescription = "Runs all affected unit tests for non Android modules."
    )
}
