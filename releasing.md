# Releasing

* Create a local release branch from `main`
```bash
git checkout main
git pull
git checkout -b release_0.1.0
```

* Update `VERSION` in `affectedmoduledetector/build.gradle` (remove `-SNAPSHOT`)
```kotlin
VERSION = 0.1.0"
```


* Commit all local changes
```
git commit -am "Prepare 0.1.0 release"
```

* Create a tag and push it
```bash
git tag v0.1.0
git push origin v0.1.0 --follow-tags
```

* Upload to Maven Central
```bash
 ./gradlew publishMavenPublicationToMavenRepository -PSONATYPE_USERNAME=<username> -PSONATYPE_PASSWORD=<password>
```

* Merge the release branch to main
```
git checkout main
git pull
git merge --no-ff release_0.1.0
```
* Update `version` in `affectedmoduledetector/build.gradle` (increase version and add `-SNAPSHOT`)
```kotlin
VERSION = "REPLACE_WITH_NEXT_VERSION_NUMBER-SNAPSHOT"
```

* Commit your changes
```
git commit -am "Prepare for development phase"
```

* Push your changes
```
git push
```
