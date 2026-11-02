# GameBridge

GameBridge is a small Android app that takes a Shortcut chosen in LaunchBox and dispatches it to the right Target App (GameNative first). LaunchBox calls it through its Custom Emulator (With Code) feature. See `GLOSSARY.md` for terms and `docs/adr/` for decisions.

## What it does

LaunchBox's Custom Emulator (With Code) starts GameBridge with a Shortcut as a `content://` URI. GameBridge reads and validates the Shortcut, then dispatches it to GameNative with an explicit intent (action `app.gamenative.LAUNCH_GAME`, `app_id`, `game_source`) and closes. Nothing is shown on a successful Dispatch. On failure, a dialog shows the Shortcut's filename and a reason, with one Close button. GameBridge requests no permissions.

Setup in LaunchBox (exact command, backup and undo): `docs/launchbox-setup.md`.

## Build

Requirements: JDK 21 and the Android SDK (setup steps in `docs/device-testing.md`). Put the SDK path in `local.properties` (gitignored): `sdk.dir=/home/<you>/Android/Sdk`.

```sh
./gradlew check                 # ktlint, Android Lint, JVM tests
./gradlew :app:assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:pixel2api33DebugAndroidTest   # instrumentation tests on a headless Android 13 emulator
```

Modules: `:core` (pure Kotlin, Shortcut interpretation) and `:app` (Android integration).

## Deploy

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build is `com.allourmedia.gamebridge.debug` and can sit beside a release build. Device procedure and rollback: `docs/device-testing.md`.

## Troubleshooting

GameBridge shows a dialog with the Shortcut's filename (when known) and one reason. The exact wording lives in the app's string resources and may differ slightly from the names below. Close (or Back) returns you to LaunchBox; retry by picking the game again.

| Reason | What it means and what to do |
|---|---|
| Meant to be started from LaunchBox (no Shortcut) | GameBridge was opened without a Shortcut, for example from the app drawer. Start games from LaunchBox. If it happens from LaunchBox, check the command in `docs/launchbox-setup.md` includes `-d {file.uri}`. |
| Wrong action | The emulator command's `-a` is not `com.allourmedia.gamebridge.action.DISPATCH`. Fix the command. |
| Not a `content://` URI | LaunchBox passed something other than `content://` (for example a file path). Use `-d {file.uri}`, not `{file.path}`. |
| The Shortcut couldn't be read | Permission missing or revoked, file gone, or a provider error. Check the game's file still exists in LaunchBox and pick it again. |
| Not a supported Shortcut | The extension is not a known Storefront extension. Check which files the platform hands to the emulator. |
| Storefront not verified yet | Steam, Epic, Amazon and Custom Shortcuts are recognised but not dispatched yet. |
| Remote Shortcuts not supported | `.art` (Artemis) Shortcuts are recognised only. Artemis is a later phase. |
| Invalid Store ID | The Shortcut content is not one positive whole number in the signed 32-bit range (GameNative writes `0` for some GOG games), or the file is over 4 KiB. |
| GameNative is not installed | Install the standard GameNative build from its GitHub releases page. |
| GameNative refused the request | Android refused the Dispatch with a security error. Make sure the standard GameNative build (`app.gamenative`) is installed and up to date. |

## Limitations

- Only GOG Shortcuts are dispatched. Steam, Epic, Amazon and Custom Shortcuts are recognised but not verified yet; Remote (Artemis) Shortcuts are recognised and reported as unsupported.
- Only the standard GameNative build (`app.gamenative`) is targeted, not the "gold" variant (`app.gamenative.gold`).
- A successful Dispatch is not proof of a Launch. GameBridge cannot observe the game starting; only a person or logcat on the device can confirm it.
- The full LaunchBox, GameBridge, GameNative path has not yet been run on the device (Gate G1).
- There is no fallback to another Target App or to a deep link.

## Enabling another Storefront

Each Storefront is one row in the Storefront table in `:core`. Enabling one means marking its row verified, adding a fixture Shortcut, and a device check that a real Dispatch reaches the game's Ready screen. No new parsing or dispatch code is needed.
