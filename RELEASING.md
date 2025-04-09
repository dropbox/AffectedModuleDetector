Releasing
========

 1. Change the version in the `gradle.properties` file to a non-SNAPSHOT verson.
 2. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 3. `git tag -a vX.Y.X -m "Version X.Y.Z"` (where X.Y.Z is the new version)
    * Run `git tag` to verify it.
 4. `git push && git push --tags`
    * This should be pushed to your fork.
 5. Create a PR with this commit and merge it.
 6. Confirm the new artifacts are present in https://s01.oss.sonatype.org/content/repositories/releases/com/dropbox/affectedmoduledetector/affectedmoduledetector/
    * If the plugin fails to publish the release, you'll need to log into Sonatype and drop any repositories under 'Staging Repositories' and re-run the CI job to publish.
 7. Go to https://github.com/dropbox/AffectedModuleDetector/releases and `Draft a new release` from the tag you just created
 7. Update the top level `gradle.properties` to the next SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. Create a PR with this commit and merge it.
