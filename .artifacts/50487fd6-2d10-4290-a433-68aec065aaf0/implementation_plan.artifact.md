# Fix Kotlin/Gradle Sync Error (JavaVersion.parse: 25.0.2)

The project is currently failing to sync because the Kotlin and AGP versions used are incompatible with the JDK 25.0.2 environment (likely the default in August 2026). The error `java.lang.IllegalArgumentException: 25.0.2` in `JavaVersion.parse` indicates that the internal version parser in the Kotlin Gradle Plugin (version 2.0.21) does not recognize or correctly parse the Java 25 version string.

## Proposed Changes

We will upgrade the core build tools to modern versions that support JDK 25.

### Dependencies and Plugins

#### [MODIFY] [libs.versions.toml](file:///D:/Arun/My-PALM/my-palm-ramp-control/gradle/libs.versions.toml)
- Upgrade `agp` to `9.3.1`
- Upgrade `kotlin` to `2.4.10`
- Upgrade `ksp` to `2.3.11`
- Upgrade `hilt` to `2.60.1`
- Upgrade `navigationFragment` and `navigationUiKtx` to `2.9.8`
- Upgrade `media3Common` and `media3Ui` to `1.6.0` (assuming latest stable)

#### [MODIFY] [gradle-wrapper.properties](file:///D:/Arun/My-PALM/my-palm-ramp-control/gradle/wrapper/gradle-wrapper.properties)
- Upgrade Gradle version to `9.6.1` to support AGP 9.3.1 and JDK 25.

### Module Configuration

#### [MODIFY] [app/build.gradle.kts](file:///D:/Arun/My-PALM/my-palm-ramp-control/app/build.gradle.kts)
- Ensure `jvmTarget` and compatibility options are set appropriately (e.g., "21" or "17" if 1.8 is too old, but 1.8 should still work). Given it's 2026, upgrading to Java 17 or 21 as the baseline is recommended.

## Verification Plan

### Automated Tests
- Trigger a Gradle Sync in Android Studio.
- Run a clean build via Gradle: `./gradlew clean assembleDebug` (after fixing JAVA_HOME if necessary, or assuming the IDE's environment is correct).

### Manual Verification
- Verify that the sync error `java.lang.IllegalArgumentException: 25.0.2` is resolved.
