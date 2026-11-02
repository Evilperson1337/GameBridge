# 05: LaunchBox setup guide, README and rollback checklist

**What to build:** A player can configure LaunchBox to use GameBridge correctly the first time, and undo it. A maintainer has a written, reversible procedure for the Gate G1 check on the Odin. See the spec's "Bridge activity contract" section and the "Player: setup" and "Device validation" user stories.

**Blocked by:** 02 (Tracer bullet: a GOG Shortcut is dispatched to GameNative)

**Status:** resolved

- [x] `docs/launchbox-setup.md` gives the exact Custom Emulator (With Code) command for both the release and the `.debug` application ID, taken from the real manifest. It explains where to find Custom Emulator (With Code) in LaunchBox (scroll to the top of the emulator picker) and how to back up and restore the Windows platform's emulator configuration.
- [x] The README covers what GameBridge does, how to build (`./gradlew check`, the debug APK task, the instrumentation task), how to deploy with `adb install -r`, troubleshooting for each error-dialog reason, and limitations (only GOG is dispatched, while Steam, Epic, Amazon and Custom Shortcuts are recognised but not verified yet; standard GameNative build only; Dispatch isn't proof of Launch). It briefly explains that enabling another Storefront means one table row, one fixture and a device check.
- [x] `docs/device-testing.md` gains the Odin validation procedure. It uses a temporary copy of the Akalabeth Shortcut and a temporary LaunchBox entry, filters logcat by the `GameBridge` tag, and marks which steps need the user's approval (per AGENTS.md).
- [x] `docs/device-testing.md` includes the rollback checklist: restore the emulator configuration, remove the temporary entry and file, uninstall the debug build, and verify original Shortcuts and game data are unchanged. It notes not to rely on any earlier `/tmp` backup.
- [x] The docs use glossary terms throughout (Shortcut, Storefront, Store ID, Target App, Dispatch and Launch).
