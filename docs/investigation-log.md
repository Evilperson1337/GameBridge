# Investigation log

Each entry says what was verified (observed in a real run) and what was assumed.

## 2026-11-02: Ticket 01, toolchain and project skeleton

### Verified (real runs on CachyOS, JDK 21.0.12, kernel 7.3.1-1-cachyos)

- Google's `commandlinetools-linux-16111833_latest.zip` unpacked to `~/Android/Sdk/cmdline-tools/latest` reports cmdline-tools 23.0. Installed via sdkmanager: platform-tools 37.0.1, build-tools 36.0.0, platforms;android-37.0, emulator 37.2.13, system-images;android-33;google_apis;x86_64 (r17). No sudo.
- Gradle 9.6.0 wrapper pinned with `distributionSha256Sum` (`bbaeb2fe...a529a01`, matches services.gradle.org's published checksum).
- With AGP 9.5.0 and Kotlin 2.4.21 (KGP on the root classpath via `alias(...) apply false`), `./gradlew clean check --rerun-tasks --no-build-cache` succeeds: ktlint 1.8.0 (plugin 14.2.0), Android Lint and the `:core` JVM test (1 test, 0 failures).
- `./gradlew :app:assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:pixel2api33DebugAndroidTest` boots a headless emulator, runs `SmokeInstrumentedTest` (1 test, 0 failures, asserts package name `com.allourmedia.gamebridge.debug`).
- No KGP/AGP compatibility warning appeared in the `check` or `assembleDebug` output. Kotlin's table lists AGP only up to 9.3.1, so AGP 9.5.0 is formally unlisted, but nothing was reported on this skeleton.

### Warnings seen in the first build

- Managed device setup prints: `The device "pixel2api33" does not specify a "testedAbi"` (default changes to arm64-v8a in AGP 10.0). Setting `testedAbi = "x86_64"` in the device config did NOT silence the message; the test still passes. Cause unknown. Assumed to be an AGP 9.5 quirk; not investigated further.
- `sdkmanager` prints a deprecation notice (replaced by `android sdk`). Verified; harmless.

### Assumed / not verified

- That an app with a larger code base will not hit AGP 9.5 + KGP 2.4.21 incompatibilities. Only the empty skeleton was built.
- The skeleton manifest has no activity yet, so installing and starting the app was not tried.
- Waydroid was not installed or tried.
