# Standalone Gradle build

This repository is normally built as part of an Android platform source tree
using `Android.mk` (see that file for the AOSP/platform build recipe). The
Gradle files at the repository root (`build.gradle`, `settings.gradle`,
`gradle.properties`, `gradlew`) add a second, standalone way to build the app
from a plain checkout, without an Android platform tree. `Android.mk` is
unaffected by this and continues to work as before.

## Requirements

- JDK 8
- Android SDK Platform 22 (`compileSdkVersion 22`)
- Android SDK Build-Tools 26.0.2

Install the SDK components with the SDK manager, e.g.:

```
sdkmanager "platforms;android-22" "build-tools;26.0.2"
```

The Gradle wrapper (`./gradlew`) downloads the pinned Gradle/Android Gradle
Plugin versions automatically; only the JDK and Android SDK need to be
installed locally.

## Configuring the SDK location

Point the build at your local Android SDK either by creating a
`local.properties` file in the repository root (not committed):

```
sdk.dir=/path/to/Android/sdk
```

or by exporting `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) in your shell
environment.

## Building

```
./gradlew assembleDebug
```

This compiles the app sources under `src/`, together with the bundled
library source trees under `library/` (`HoloGraphLibrary`, `UndoBar`,
`WilliamChart`, `floatingactionbutton`, `ldrawer`, `showcase`), as a single
application module — the same layout used by `Android.mk`'s
`LOCAL_SRC_FILES`/`LOCAL_RESOURCE_DIR`. The `aaptOptions.additionalParameters
--extra-packages` setting in `build.gradle` reproduces `Android.mk`'s
`LOCAL_AAPT_FLAGS`, so each bundled library's own `R` class is still
generated correctly.

`minSdkVersion`, `targetSdkVersion`, `versionCode` and `versionName` are not
duplicated in `build.gradle`; they come from `AndroidManifest.xml`.