# Releasing

* Create a local release branch from `main`
```bash
git checkout main
git pull
git checkout -b release_0.1.0
```

* Update `version` in `buildSrc/build.gradle.kts` (remove `-SNAPSHOT`)
```kotlin
version = 0.1.0"
```


* Commit all local changes
```
git commit -am "Prepare 0.1.0 release"
```

* Create a tag and push it
```bash
git tag v0.1.0
git push origin v0.1.0
```

* Upload to Gradle Plugin Portal
```bash
./gradlew -b buildSrc/build.gradle.kts publishPlugins
```

* Merge the release branch to master
```
git checkout master
git pull
git merge --no-ff release_0.1.0
```
* Update `version` in `buildSrc/build.gradle.kts` (increase version and add `-SNAPSHOT`)
```kotlin
version = "REPLACE_WITH_NEXT_VERSION_NUMBER-SNAPSHOT"
```

* Commit your changes
```
git commit -am "Prepare for next development iteration"
```

* Push your changes
```
git push
```
