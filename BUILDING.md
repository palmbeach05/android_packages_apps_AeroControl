# Building AeroControl

## Requirements

The standalone Gradle build requires:

- Java 8. The checked-in build uses Gradle 4.1 and Android Gradle Plugin 3.0.1.
- Android SDK command-line tools.
- Android SDK Platform 22.
- Android SDK Build-Tools 26.0.2.
- Accepted Android SDK licenses.

Install the required SDK packages and accept the licenses:

sh
sdkmanager "platform-tools" "platforms;android-22" "build-tools;26.0.2"
yes | sdkmanager --licenses
