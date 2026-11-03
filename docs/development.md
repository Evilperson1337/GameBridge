# Development

GameBridge is a small Android app that dispatches a Shortcut chosen in LaunchBox to a Target App. Terms: `GLOSSARY.md`. Decisions: `docs/adr/`. Background: `docs/overview/GameBridge_Implementation_Handoff.md`. Rules for contributors and agents: `AGENTS.md`.

## Build

Requirements: JDK 21 and the Android SDK (setup steps in `docs/device-testing.md`). Put the SDK path in `local.properties` (gitignored): `sdk.dir=/home/<you>/Android/Sdk`.

```sh
./gradlew check                 # ktlint, Android Lint, JVM tests
./gradlew :app:assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:pixel2api33DebugAndroidTest   # instrumentation tests on a headless Android 13 emulator
```

Modules: `:core` (pure Kotlin: Shortcut interpretation, Setup command, failure reasons) and `:app` (Android integration: the bridge activity that dispatches, and the setup screen opened from the app drawer).

Without the signing secrets the release build is unsigned and `./gradlew check` still passes. Tagged releases are built and signed by `.github/workflows/release.yml`. Before tagging, set `versionName` in `app/build.gradle.kts` to the tag without the `v` (the workflow fails otherwise) and increment `versionCode` for every release; Android refuses to install an update whose `versionCode` is not higher. The workflow does not check `versionCode`.

## Deploy to a device

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build is `com.allourmedia.gamebridge.debug` and can sit beside a release build; each shows its own Setup command on its setup screen. Device procedure and rollback: `docs/device-testing.md`.

## Enabling another Storefront

Each Storefront is one row in the Storefront table in `:core`. Enabling one means marking its row verified, adding a fixture Shortcut, and a device check where a person sees the game start in the Target App after a real Dispatch (that is a Launch, which GameBridge cannot observe). No new parsing or dispatch code is needed. The README support table moves a row to verified only with a device observation recorded in `docs/investigation-log.md`.

## Documents

- `docs/launchbox-setup.md`: the player's LaunchBox guide.
- `docs/device-testing.md`: SDK, emulator, physical device, rollback.
- `docs/investigation-log.md`: what was verified and what was assumed.
- `docs/research/`: dated research notes.

## Release signing

Releases are signed in GitHub Actions from four repository secrets (`GAMEBRIDGE_KEYSTORE_BASE64`, `GAMEBRIDGE_KEYSTORE_PASSWORD`, `GAMEBRIDGE_KEY_ALIAS`, `GAMEBRIDGE_KEY_PASSWORD`). `scripts/setup-release-signing.sh` walks the maintainer through creating the keystore outside the repo, backing it up offline, and setting those secrets. It never writes a password to disk. Increment `versionCode` for every release.
