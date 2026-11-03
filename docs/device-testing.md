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

## Odin validation procedure (Gate G1)

Gate G1 is closed (see `docs/investigation-log.md`, "Gate G1: closed"); this is the written procedure, kept for re-checks. It was not followed step by step.

Goal: LaunchBox -> GameBridge -> GameNative -> Akalabeth Ready screen, using a temporary copy of the Akalabeth Shortcut and a temporary LaunchBox entry, with nothing original modified. Terms are in `GLOSSARY.md`. A Dispatch is only a hand-off; a person must confirm the Launch (the Ready screen).

Steps marked **ASK** need the user's explicit approval first (AGENTS.md). Steps marked **standing** are covered by the standing approval (install/uninstall of `com.allourmedia.gamebridge.debug`, read logcat).

1. Record the original state: the SHA-256 of the original Akalabeth Shortcut (content `1207666073`) and of LaunchBox's configuration XML, somewhere that survives a reboot (not `/tmp`). Read-only. **ASK** if reading these needs more than read access.
2. **ASK** Back up the Windows platform's emulator configuration and LaunchBox XML (see `docs/launchbox-setup.md`). Note the original emulator selection.
3. **standing** Build and install: `./gradlew :app:assembleDebug`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4. **ASK** Create a temporary copy of the Akalabeth Shortcut (a new file, never the original) and record its path.
5. **ASK** In LaunchBox, add a temporary entry that points at the temporary copy, and set the Windows platform's emulator to Custom Emulator (With Code) using the `.debug` command from `docs/launchbox-setup.md`.
6. **standing** Start logcat filtered by the tag: `adb logcat -s GameBridge`.
7. Pick the temporary entry in LaunchBox. Expect no GameBridge UI, GameNative opening, and a debug log line like `storefront=GOG storeId=1207666073 scheme=content dispatch=Dispatched`.
8. A person confirms Akalabeth reaches its Ready screen. Log lines alone do not prove a Launch. Record the result in `docs/investigation-log.md` (verified vs assumed).
9. Optional check: open GameBridge from the app drawer and confirm the setup screen (readiness line, three app tiles, steps, Setup command and Copy). It dispatches nothing.
10. Run the rollback checklist below.

## Rollback checklist

Do not rely on an earlier `/tmp` backup: the previous investigation cleaned its temporary artifacts. Use the backup you made in step 2.

- [ ] **ASK** Restore the Windows platform's original emulator selection and configuration (and, with LaunchBox closed, the backed-up XML if you backed up that way). Compare the XML's SHA-256 to the recorded one.
- [ ] **ASK** Remove the temporary LaunchBox entry.
- [ ] **ASK** Delete the temporary Shortcut copy (only that file).
- [ ] **standing** Uninstall the debug build: `adb uninstall com.allourmedia.gamebridge.debug`.
- [ ] Verify the original Akalabeth Shortcut's SHA-256 and contents are unchanged, and that game data and saves were not touched.
- [ ] Verify GameNative and LaunchBox were not modified or reinstalled, and no unrelated LaunchBox entry changed.
