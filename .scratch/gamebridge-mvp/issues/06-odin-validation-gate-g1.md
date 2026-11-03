# 06: Odin validation (Gate G1)

**What to build:** Confirm on the real device that picking Akalabeth in LaunchBox goes through GameBridge to GameNative and reaches the game's Ready screen. This is the first confirmed Launch, observed by a person, not inferred from a Dispatch. Then return the device to its original state. Follow the procedure and rollback checklist in `docs/device-testing.md`.

**Blocked by:** 04 (Full Shortcut interpretation rules), 05 (LaunchBox setup guide, README and rollback checklist)

**Status:** ready-for-human (superseded: Gate G1 was closed on other evidence, see `docs/investigation-log.md`, "Gate G1: closed"; the criteria below were not performed as written)

- [ ] The debug build is installed on the Odin with `adb install -r` (standing approval per AGENTS.md).
- [ ] With the user's explicit go-ahead, the LaunchBox emulator configuration is backed up (location recorded), and a temporary Akalabeth Shortcut copy and temporary LaunchBox entry are created (paths recorded).
- [ ] With the user's explicit go-ahead, the temporary entry uses Custom Emulator (With Code) with the debug command from `docs/launchbox-setup.md`.
- [ ] Picking the entry dispatches to GameNative, and Akalabeth reaches its Ready screen, confirmed by the user and by a filtered `GameBridge` logcat excerpt.
- [ ] One failure path is spot-checked on the device (for example a temporary malformed Shortcut copy shows the error dialog, and Close returns to LaunchBox).
- [ ] Optional, and only if DroidDeck is installed and the user approves: a temporary LaunchBox entry points straight at DroidDeck, without GameBridge, using the configuration in `docs/research/droiddeck-target-app.md` (`.FrontEndActivity`, `ACTION_VIEW`, `-d {file.uri}`) with a temporary `.steam` Shortcut copy. Record whether the game starts; that decides whether DroidDeck support in GameBridge is worth building. Remove the entry and the copy afterwards.
- [ ] The rollback checklist is performed. The original Akalabeth Shortcut contents, game data and unrelated LaunchBox entries are verified unchanged.
- [ ] `docs/investigation-log.md` records the evidence (logcat excerpt, what was observed, what's still unverified), with personal and device identifiers removed, plus a recommendation to proceed to Phase 2 or stop.
