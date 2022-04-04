package com.dropbox.affectedmoduledetector.plugin

/**
 * For creating a custom task which will be run only if module was affected
 * just create enum which implements this interface.
 *
 * Your enum must override all this variable
 */
interface AffectedModuleTaskType {

    /**
     * Console command `./gradlew commandByImpact` which will run the original
     * command `./gradlew originalCommand` on modules affected by diff changes.
     */
    val commandByImpact: String

    /**
     * The original console command `./gradlew originalCommand` that does something.
     * Example:
     *     - :connectedDebugAndroidTest
     *     - :assembleDebugAndroidTest
     *     - :detekt
     */
    val originalGradleCommand: String

    /**
     * Description of new gradle task
     */
    val taskDescription: String
}
