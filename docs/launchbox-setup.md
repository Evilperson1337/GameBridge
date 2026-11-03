# LaunchBox setup guide

This guide configures LaunchBox for Android to hand a Shortcut to GameBridge, which dispatches it to GameNative (the Target App). Terms are defined in `GLOSSARY.md`.

## How to read this guide

Every LaunchBox step is labelled:

- **Verified**: shown by the `Windows.xml` readback in `docs/investigation-log.md` ("Working LaunchBox configuration, read from the device"). That readback shows the result of a working setup, not the screens used to reach it.
- **Assumed**: not observed. The LaunchBox screen names, menu paths and field names are not recorded anywhere in this repo, so treat them as hints and check them on your device.

What the readback verified: on the Windows platform, `DefaultEmulator` is `Custom Emulator (With Code)`, `CustomEmulatorLaunchArguments` holds the Setup command, and the games' per-game emulator overrides are empty. A Dispatch from LaunchBox through GameBridge to GameNative was also seen working with an Epic Shortcut (reported by the user; see the log). A successful Dispatch does not prove the game started (a Launch).

## Warning: this affects every game on the Windows platform

The supported setup replaces the Windows platform's default emulator. After it, every Windows game that does not have its own emulator override goes through GameBridge, not only your GameNative games. Games on other Windows emulators or launchers will stop working until you undo the change or give them a per-game override (per-game overrides are not verified, see "Not verified").

## 1. Back up your current settings

Verified: GameBridge never edits LaunchBox, Shortcuts, games, saves or GameNative. Undoing the setup means putting your old settings back (see "Undoing the setup").

Before changing anything, write down or screenshot the Windows platform's current emulator and its settings. Assumed: these are in LaunchBox's emulator or platform settings; the menu path is not verified.

For a verified file-level backup, see "Advanced: file backup" at the end.

## 2. Get the Setup command

Open GameBridge from the app drawer. Its setup screen shows the exact Setup command for the build you installed, with a Copy button, so you do not need to type it. It also shows whether GameNative, Artemis and LaunchBox are installed and a three-line checklist. The screen only shows and copies; it dispatches nothing.

If you cannot use the screen, the commands are below. Use the one that matches your build.

Release build (application ID `com.allourmedia.gamebridge`):

```
-n com.allourmedia.gamebridge/com.allourmedia.gamebridge.BridgeActivity -a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}
```

Debug build (application ID `com.allourmedia.gamebridge.debug`):

```
-n com.allourmedia.gamebridge.debug/com.allourmedia.gamebridge.BridgeActivity -a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}
```

Notes:

- The debug command was read back byte for byte from `Windows.xml` on a working setup (verified). The release command has the same shape and is pinned by the core test `SetupCommandTest`; it has not been read back from a device.
- The activity class is written out in full on purpose. The Kotlin package stays `com.allourmedia.gamebridge` for debug builds while the application ID gains `.debug`, so a short `.BridgeActivity` would point at the wrong class.
- The command ends with `-d {file.uri}`. Do not add `{file.path}`: GameBridge reads the Shortcut through the `content://` URI only and rejects other schemes.

## 3. Configure the Windows platform

1. Set the Windows platform's default emulator to Custom Emulator (With Code). Verified as the end state (`DefaultEmulator`). Assumed: where this is done in LaunchBox. Custom Emulator (With Code) is at the top of the emulator picker in LaunchBox 1.23 per the handoff document; the other screens are not recorded. LaunchBox's own guide: https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code
2. Paste the Setup command into the field LaunchBox labels "launch arguments". Verified as the end state (`CustomEmulatorLaunchArguments`). Assumed: the field's label and screen.
3. Clear any per-game emulator override on the Windows games you want to go through GameBridge, for example an old `GameNative` entry, so that the platform setting applies. Assumed: the screens used to clear it were not recorded. Verified: in the working setup the games' `EmulatorOverride` values are empty, but whether they were cleared by hand or by LaunchBox is not known.
4. Import the Shortcuts (`.gog`, `.steam`, `.epic`, `.amazon`, `.pcgame`) as the games' files. Remote Shortcuts (`.art`, made by Artemis) are imported the same way, and the same Setup command works for them. GameBridge does not read a `.art` file; it dispatches it to Artemis (`com.limelight.noir`), which must be installed. Remote Shortcuts are enabled but untested through LaunchBox (see the README support table). Verified: imported games have `ApplicationPath` set to the Shortcut file (seen for `.epic`, `.gog` and `.amazon`). Assumed: the import screens.

## 4. Using it

Pick a game. If the Dispatch succeeds GameBridge shows nothing and GameNative opens. If not, GameBridge shows a dialog with one reason and a Close button; press Close and you are back in LaunchBox. To retry, pick the game again. Only GOG and Epic have device evidence so far; Steam, Amazon and Custom are enabled and not yet verified.

The built-in GameNative entry in LaunchBox fails on these Shortcuts with "ROM file ... could not be found" (observed on the Odin, per the handoff document, cause unknown). That is why the Custom Emulator is used instead.

## If it fails: what LaunchBox shows

Work through these in order. They list what was seen, not proven causes.

1. **GameBridge shows a dialog.** The Dispatch reached GameBridge, so the LaunchBox command works; the dialog's reason says what is wrong. See the README troubleshooting table.
2. **LaunchBox says the launching process failed**, "most likely because the emulator developer needs to update Custom Emulator (With Code) for recent versions of Android", and that it will try launching the emulator without a game. Seen in the G1 attempt (`docs/gamebridge-g1-launchbox-test-report-20261102.md`). In that attempt the Windows platform's command still pointed at an earlier probe app that was not installed, and no GameBridge log output appeared. That is the observed explanation there; the message itself does not name its cause. Check that:
   - the platform's default emulator is Custom Emulator (With Code) and the command is the current Setup command, pasted whole;
   - the application ID in the command matches the build installed (`.debug` or not);
   - the game has no per-game emulator override;
   - GameBridge is installed.
3. **An Android permission screen appears** (the G1 attempt showed `GrantPermissionsActivity` for the Calendar). GameBridge requests no permissions, and the cause of that screen was not established. Do not grant anything on the strength of it. Go back, and recheck the points in item 2, especially a stale command left over from an earlier test.
4. **Nothing happens, or GameNative opens but the game does not start.** A Dispatch is not a Launch; GameBridge cannot see whether the game started. Check the game in GameNative itself. After exiting an Epic game a blank GameNative screen was seen (GameNative's own behaviour, cause unknown).

## Undoing the setup

1. Restore the previous emulator and settings for the Windows platform from your note or screenshot. Assumed: done through the same LaunchBox screens; not verified.
2. Restore any per-game overrides you cleared. Assumed.
3. Optionally uninstall GameBridge: `adb uninstall com.allourmedia.gamebridge` (or `com.allourmedia.gamebridge.debug`), or remove it in Android settings.
4. Nothing else needs reverting.

## Not verified

- Per-game emulator overrides pointing at GameBridge.
- Platforms other than Windows.
- Which LaunchBox screens and field names are used for any step above.
- The release Setup command read back from a device.
- GOG re-checked through LaunchBox with the final setup, and Steam, Amazon and Custom Storefronts.
- Remote Shortcuts (`.art`) picked in LaunchBox and dispatched to Artemis.

## Advanced: file backup

This is the procedure for a verified backup. Verified: LaunchBox keeps the Windows platform settings in `/storage/emulated/0/LaunchBox/Data/Platforms/Windows.xml` on the Odin (read with `adb pull`). Keep the backup somewhere that survives a reboot, not `/tmp`.

1. With LaunchBox closed, copy the file off the device:
   `adb pull /storage/emulated/0/LaunchBox/Data/Platforms/Windows.xml ~/launchbox-backup/`
2. Record its checksum: `sha256sum ~/launchbox-backup/Windows.xml > ~/launchbox-backup/SHA256SUMS`
3. Do not edit the live file while LaunchBox is using it.

To restore, put the settings back through LaunchBox, or, with LaunchBox closed, `adb push` the backed-up file over the live one and compare its `sha256sum` to the recorded value. Assumed: that restoring by pushing the file works; it was not tried.
