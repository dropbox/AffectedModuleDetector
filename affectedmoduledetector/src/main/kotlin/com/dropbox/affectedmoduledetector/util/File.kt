package com.dropbox.affectedmoduledetector.util

import java.io.File

/**
 * Converts a [String] representation of a relative [File] path to sections based on the OS
 * specific separator character.
 */
fun String.toPathSections(rootProjectDir: File, gitRootDir: File): List<String> {
    val realSections = toOsSpecificPath()
        .split(File.separatorChar)
        .toMutableList()
    val projectRelativeDirectorySections = rootProjectDir
        .toRelativeString(gitRootDir)
        .split(File.separatorChar)
    for (directorySection in projectRelativeDirectorySections) {
        if (realSections.isNotEmpty() && realSections.first() == directorySection) {
            realSections.removeAt(0)
        } else {
            break
        }
    }
    return realSections.toList()
}
