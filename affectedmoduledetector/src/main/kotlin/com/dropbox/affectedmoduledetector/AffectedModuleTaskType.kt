package com.dropbox.affectedmoduledetector

import java.io.Serializable

/**
 * For creating a custom task which will be run only if module was affected
 * just override fields in your data structure which implements this interface.
 *
 * Your data structure must override all this variable
 */
interface AffectedModuleTaskType : Serializable {

    /**
     * Console command `./gradlew [commandByImpact]` which will run the original
     * command `./gradlew originalCommand` on modules affected by diff changes.
     */
    val commandByImpact: String

    /**
     * The original console command `./gradlew [originalGradleCommand]` that does something.
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
