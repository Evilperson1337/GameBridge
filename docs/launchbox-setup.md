# LaunchBox setup guide

This guide configures LaunchBox for Android to hand a Shortcut to GameBridge, which dispatches it to GameNative (the Target App). Terms are defined in `GLOSSARY.md`.

Status: the command below is built from the real manifest and code, but it has not yet been run through LaunchBox on the Odin (Gate G1, ticket 06). The LaunchBox steps marked "assumed" come from the handoff document and the investigation log, not from a fresh run. See `docs/investigation-log.md`.

## Before you start: back up

Back up the Windows platform's emulator configuration before changing anything, and keep the backup somewhere that survives a reboot (not `/tmp`).

1. Note the Windows platform's current emulator and its settings (screenshots help). Assumed: this is under the platform's emulator settings in LaunchBox's settings menu; the exact menu path is not verified here.
2. Copy LaunchBox's configuration XML (the file that holds the platform and emulator settings) to a safe place, for example with `adb pull`, and record its SHA-256. The file's name and location are not verified here; find them on your device first. Do not edit the original while LaunchBox is using it.

To restore: put the same emulator and settings back through LaunchBox, or, with LaunchBox closed, copy the backed-up XML over the live file and compare the SHA-256 to the one you recorded.

## The command

In LaunchBox, add a Custom Emulator (With Code) for the Windows platform and use one of these command lines. The command always uses `-n` (explicit component), `-a` (GameBridge's action) and `-d {file.uri}` (the Shortcut as a `content://` URI).

Release build (application ID `com.allourmedia.gamebridge`):

```
-n com.allourmedia.gamebridge/com.allourmedia.gamebridge.BridgeActivity -a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}
```

Debug build (application ID `com.allourmedia.gamebridge.debug`):

```
-n com.allourmedia.gamebridge.debug/com.allourmedia.gamebridge.BridgeActivity -a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}
```

Notes:

- The activity class is written out in full on purpose. The Kotlin package stays `com.allourmedia.gamebridge` for debug builds, while the application ID gains `.debug`, so the short form `.BridgeActivity` would point at the wrong class in the debug build.
- The action is the same in both builds: `com.allourmedia.gamebridge.action.DISPATCH` (from `app/src/main/AndroidManifest.xml`).
- Do not add `{file.path}`. GameBridge reads the Shortcut through the `content://` URI only and rejects any other scheme.

## Finding Custom Emulator (With Code)

Scroll to the top of LaunchBox's emulator picker. Custom Emulator (With Code) is listed there (seen in LaunchBox 1.23 on the Odin; recorded in the handoff document). The remaining screens and field names (where the command is typed, how the platform is bound to the emulator, whether the Shortcut extensions need adding) are assumed, not verified in this repo. Check them against LaunchBox's own guide: https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code

## Using it

One emulator configuration on the Windows platform covers all your GameNative games. Import the Shortcuts (`.gog` and so on) into LaunchBox as the games' files, then pick a game. If the Dispatch succeeds GameBridge shows nothing and GameNative opens. If not, GameBridge shows a dialog; press Close and you are back in LaunchBox. To retry, pick the game again.

The built-in GameNative entry fails on these Shortcuts with "ROM file could not be found" (observed on the Odin, per the handoff document); that is why the Custom Emulator is used instead.

## Undoing the setup

1. Restore the previous emulator and settings for the Windows platform (see "Before you start").
2. Optionally uninstall GameBridge: `adb uninstall com.allourmedia.gamebridge` (or `com.allourmedia.gamebridge.debug` for the debug build).
3. GameBridge never modifies Shortcuts, games, saves, GameNative or LaunchBox, so nothing else needs reverting.
