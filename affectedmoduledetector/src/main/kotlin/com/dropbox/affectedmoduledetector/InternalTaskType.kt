package com.dropbox.affectedmoduledetector

internal enum class InternalTaskType(
    override val commandByImpact: String,
    override val originalGradleCommand: String,
    override val taskDescription: String
) : AffectedModuleTaskType {

    ANDROID_TEST(
        commandByImpact = "runAffectedAndroidTests",
        originalGradleCommand = AffectedTestConfiguration.DEFAULT_ANDROID_TEST_TASK,
        taskDescription = "Runs all affected Android Tests. Requires a connected device."
    ),

    ASSEMBLE_ANDROID_TEST(
        commandByImpact = "assembleAffectedAndroidTests",
        originalGradleCommand = AffectedTestConfiguration.DEFAULT_ASSEMBLE_ANDROID_TEST_TASK,
        taskDescription = "Assembles all affected Android Tests. Useful when working with device labs."
    ),

    ANDROID_JVM_TEST(
        commandByImpact = "runAffectedUnitTests",
        originalGradleCommand = AffectedTestConfiguration.DEFAULT_JVM_TEST_TASK,
        taskDescription = "Runs all affected unit tests."
    ),

    JVM_TEST(
        commandByImpact = "", // inner type. This type doesn't registered in gradle
        originalGradleCommand = AffectedTestConfiguration.DEFAULT_NON_ANDROID_JVM_TEST_TASK,
        taskDescription = "Runs all affected unit tests for non Android modules."
    )
}
