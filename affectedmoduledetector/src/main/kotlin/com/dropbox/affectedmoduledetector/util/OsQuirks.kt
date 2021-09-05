package com.dropbox.affectedmoduledetector.util

import java.io.File

/**
 * Returns an OS specific path respecting the separator character for the operating system.
 *
 * The Git client appears to only talk Unix-like paths however the Gradle client understands all OS path variantions.
 * This causes issues on systems other than those that use the "/" path character i.e. Windows. Therefore we need to
 * normalise the path.
 */
fun String.toOsSpecificPath(): String {
    return this.split("/").joinToString(File.separator)
}

fun String.toOsSpecificLineEnding(): String {
    return this.replace("\n", System.lineSeparator())
}
