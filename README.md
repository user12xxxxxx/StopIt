# stopIt

An Android app that puts a short pause between you and the apps you open on autopilot.

> [!WARNING]
> This app was built with the assistance of AI (Claude). The code has been tested on an emulator, but it may contain bugs or rough edges. Review it before relying on it, and please report anything you find.

Pick the apps to guard. When one of them opens, stopIt covers it with a fullscreen pause screen for a random number of seconds within a range you choose. When the countdown ends you can close the app, or open it for a limited visit (2, 5 or 15 minutes, or no limit).

## Features

- Guard any launchable app, with search and category filters
- Random pause length from a range you set (1 to 30 seconds), plus a custom message on the pause screen
- Visit limits: after the time runs out the pause comes back
- Home screen with today's counters (walked away, opened anyway) and a week or month trend chart
- Colours follow your wallpaper (Material You), with an optional pure black pause screen for AMOLED displays

## Requirements

- Android 12 (API 31) or newer
- Two permissions granted in system Settings, which the app walks you through on first launch:
  - **Usage access**, to see which app is in the foreground
  - **Display over other apps**, to show the pause screen on top

stopIt has no internet permission. Settings and history stay on the device.

## Building

Requires JDK 17 or newer (Android Studio's bundled JBR works) and an Android SDK with platform 37 and build-tools 37.0.0.

```sh
./gradlew assembleDebug        # build
./gradlew installDebug         # install on a connected device or emulator
./gradlew testDebugUnitTest    # unit tests
./gradlew lint                 # Android lint
```

Release builds are signed with a key read from `~/.android/stopit-keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without that file, `./gradlew assembleRelease` produces an unsigned APK.

## Credits

The guard card uses [Roboto Flex](https://github.com/googlefonts/roboto-flex), licensed under the SIL Open Font License 1.1 (`licenses/RobotoFlex-OFL.txt`).

## License

GPL-3.0. See `LICENSE`.
