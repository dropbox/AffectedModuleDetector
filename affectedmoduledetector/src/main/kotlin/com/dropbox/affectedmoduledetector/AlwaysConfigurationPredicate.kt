package com.dropbox.affectedmoduledetector

import org.gradle.api.artifacts.Configuration
import java.util.function.Predicate

/**
 * Default implementation of a [Configuration] [Predicate] that always returns true, indicating
 * that all configurations should be considered.
 */
internal class AlwaysConfigurationPredicate : Predicate<Configuration> {
    override fun test(t: Configuration): Boolean {
        return true
    }
}
