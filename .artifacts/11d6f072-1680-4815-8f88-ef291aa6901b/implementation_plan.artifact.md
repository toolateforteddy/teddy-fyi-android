# Upgrade to JDK 21 for SDK 36 and Robolectric 4.16 Compatibility

To support targeting Android 16 (API 36) and running local tests with Robolectric 4.16, the project should be upgraded to use JDK 21. Robolectric 4.16 requires JDK 21 when targeting SDK 36.

## Proposed Changes

### [app]

#### [MODIFY] [build.gradle](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/build.gradle)
- Update `sourceCompatibility` to `JavaVersion.VERSION_21`.
- Update `targetCompatibility` to `JavaVersion.VERSION_21`.
- Update Kotlin `jvmTarget` to `JvmTarget.JVM_21`.

## Verification Plan

### Automated Tests
- Run `gradle_sync` to ensure the IDE recognizes the new JVM target.
- Run `./gradlew test` (or a subset) to verify that Robolectric tests can run with the new configuration.
