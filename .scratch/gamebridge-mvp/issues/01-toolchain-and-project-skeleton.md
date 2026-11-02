# 01: Toolchain and project skeleton (Gate G0)

**What to build:** A developer on CachyOS can go from a clean checkout to a green build. The Android SDK is installed in the user's home directory without sudo. The Gradle project at the repo root has a pure Kotlin `:core` module and an Android `:app` module, with every version pinned. One command runs formatting checks, Android Lint and the tests; another builds the debug APK. A smoke instrumentation test runs on a headless Android 13 emulator that Gradle manages itself. See the spec's "Toolchain (pinned)" and "Modules" sections: `.scratch/gamebridge-mvp/spec.md`.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Android command-line tools 23.0 are installed in `~/Android/Sdk`, with platform-tools, build-tools 36, platform 37, the emulator, and an Android 13 (API 33) system image. No sudo is used and nothing is installed system-wide.
- [ ] The SDK location is in a gitignored `local.properties`. Ask the user before adding `ANDROID_HOME` to their fish config.
- [ ] The Gradle 9.6.0 wrapper is committed, and AGP 9.5.0 and Kotlin 2.4.21 are pinned in one version catalogue. `:app` uses AGP's built-in Kotlin support, and the Kotlin Gradle plugin is raised to 2.4.21 on the root build classpath so both modules share it.
- [ ] `:app` uses application ID `com.allourmedia.gamebridge` (ADR 0001), with a `.debug` suffix for debug builds, compileSdk 37, targetSdk 36 and minSdk 30. It compiles to Java 17 bytecode while the build runs on JDK 21. No Compose.
- [ ] ktlint (via its Gradle plugin) and Android Lint are wired in, and `./gradlew check` passes from a clean checkout, including at least one placeholder JVM test in `:core`.
- [ ] `./gradlew :app:assembleDebug` produces an APK.
- [ ] A Gradle-managed Android 13 device runs a placeholder instrumentation test headlessly, and the task name is documented.
- [ ] `.gitignore` excludes build outputs, `local.properties`, IDE files, APKs and device-specific files.
- [ ] `docs/device-testing.md` documents the SDK and emulator setup commands, with Waydroid noted as optional. `docs/investigation-log.md` exists and records any warnings from the first build (AGP 9.5 is newer than Kotlin's formally listed compatibility).
- [ ] A README stub describes what GameBridge is and how to build it.
