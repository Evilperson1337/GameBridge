# Device and emulator testing

## Android SDK without sudo (CachyOS)

Everything lives in `~/Android/Sdk`. Nothing is installed system-wide. JDK 21 must already be present.

```sh
mkdir -p ~/Android/Sdk/cmdline-tools
curl -LO https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip   # cmdline-tools 23.0
unzip commandlinetools-linux-16111833_latest.zip -d /tmp/cmdline
mv /tmp/cmdline/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

SDKM=~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager
yes | $SDKM --licenses
$SDKM "platform-tools" "build-tools;36.0.0" "platforms;android-37.0" "emulator" \
      "system-images;android-33;google_apis;x86_64"
```

`sdkmanager` prints a deprecation notice in favour of the `android sdk` command; it still works.

Tell Gradle where the SDK is by creating `local.properties` in the repo root (gitignored, never commit it):

```properties
sdk.dir=/home/<you>/Android/Sdk
```

`ANDROID_HOME` is not required for the Gradle build. Setting it in a shell profile is optional and is the user's decision.

## Gradle-managed emulator (headless)

`:app` declares a managed device `pixel2api33` (Pixel 2, API 33, `google` image, x86_64). Gradle starts and stops the emulator itself, with no window.

```sh
./gradlew :app:pixel2api33DebugAndroidTest
```

Results: `app/build/reports/androidTests/managedDevice/debug/`. Requires `/dev/kvm` to be accessible to your user. Only instrumentation tests under `app/src/androidTest` run there.

## Physical device (AYN Odin 2 Portal)

Only GameBridge's own debug build (`com.allourmedia.gamebridge.debug`) may be installed with `adb install` / `adb uninstall`. Ask before installing anything else or touching LaunchBox configuration.

## Waydroid (optional)

Waydroid is available in the Arch `extra` repo but is not needed for any ticket. The Gradle-managed device is the supported way to run instrumentation tests. Waydroid could be used for manual poking only; it is not set up or verified here.
