# GameBridge

GameBridge is a small Android app that takes a Shortcut chosen in LaunchBox and dispatches it to the right Target App (GameNative first). LaunchBox calls it through its Custom Emulator (With Code) feature. See `GLOSSARY.md` for terms and `docs/adr/` for decisions.

Status: project skeleton only. There is no bridge activity yet.

## Build

Requirements: JDK 21 and the Android SDK (setup steps in `docs/device-testing.md`). Put the SDK path in `local.properties` (gitignored): `sdk.dir=/home/<you>/Android/Sdk`.

```sh
./gradlew check                 # ktlint, Android Lint, JVM tests
./gradlew :app:assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:pixel2api33DebugAndroidTest   # instrumentation tests on a headless Android 13 emulator
```

Modules: `:core` (pure Kotlin, Shortcut interpretation) and `:app` (Android integration).
