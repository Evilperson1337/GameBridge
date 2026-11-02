# GameBridge G1 LaunchBox Test Report

**Date:** 2026-11-02  
**Device:** AYN Odin 2 Portal, Android 13; USB ADB serial `<device serial>`  
**Applications:** LaunchBox 1.23 (`com.unbrokensoftware.launchbox`), GameNative 1.3.0 (`app.gamenative`), GameBridge debug build (`com.allourmedia.gamebridge.debug`)

## Purpose

Test whether a per-game LaunchBox entry for a GameNative-exported `.gog` shortcut can be configured to invoke GameBridge with the selected shortcut reference. This is a follow-up to the earlier successful LaunchBox diagnostic receiver test documented in [the general LaunchBox → GameNative investigation](launchbox-gamenative-investigation-20261101.md). That earlier test used a separate diagnostic package, `dev.launchboxprobe`; it does not establish that this GameBridge G1 attempt dispatched successfully.

## Test setup and changes

Before this test, the user supplied the following device state:

- GameBridge debug build installed.
- Temporary shortcut at `/storage/emulated/0/Download/gamebridge-g1/GBTemp_Akalabeth.gog`; contents are the Akalabeth ID `1207666073`.
- Original LaunchBox Windows and Settings XML and original Akalabeth shortcut backed up under `<backup folder>/`, with `SHA256SUMS`.
- No prior changes to LaunchBox or `Games/pc` for this test.

The backup hashes were checked with `sha256sum -c SHA256SUMS` before interacting with LaunchBox; all three files passed. The backed-up Windows XML showed the pre-test entries ABZÛ and Broforce using the `GameNative` emulator override. Broforce’s `CustomEmulator*` fields were blank.

Using LaunchBox’s **Add Game** screen, a temporary entry was saved:

| Field | Value |
|---|---|
| Title | `GBTemp_Akalabeth` |
| Platform | `Windows` |
| Startup File | `/storage/emulated/0/Download/gamebridge-g1/GBTemp_Akalabeth.gog` |
| Startup Uri File (Scoped Storage) | `(None)` |

This added one record to LaunchBox. No original shortcut or game installation/save was edited. The temporary entry and shortcut remain in place at the time of this report.

## Attempt and observed results

The filtered log stream was started with:

```bash
adb -s <device serial> logcat -s GameBridge
```

During the attempted launch, Android showed a system permission screen associated with the Calendar task. No permission was granted by the agent. The foreground activity was observed as `com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity`; the UI was then returned to LaunchBox.

LaunchBox displayed this error:

> We tried launching the game with this emulator, but the launching process failed, most likely because the emulator developer needs to update Custom Emulator (With Code) for recent versions of Android.
>
> LaunchBox will now try launching Custom Emulator (With Code) without running any games.

After dismissing the notice, LaunchBox returned to the `GBTemp_Akalabeth` detail page. The `GameBridge`-filtered log stream produced no output during the inspected interval.

### Result

**No successful GameBridge dispatch was demonstrated.** There is no log evidence that `BridgeActivity` received an intent, and no captured intent action, data URI, extras, grant flags, or receiver-side file-read result from this attempt. The Calendar permission screen and LaunchBox’s emulator failure are evidence of an unsuccessful/incorrect launch path, but they do not identify the exact cause. Do not infer that Calendar was launched by GameBridge or that the shortcut was passed to it.

## Configuration inspection

LaunchBox UI was inspected after the error:

- The long-press game menu exposed **Play**, **Edit Metadata**, **Edit Media**, and other game actions.
- The **Edit Game** form showed title/platform/startup-file fields and metadata fields; no custom-emulator launch fields were found there.
- The navigation drawer’s **Manage Emulators** screen showed global management for RetroArch and Dolphin. It did not expose this game’s custom command.
- The current LaunchBox detail screen showed `GBTemp_Akalabeth` under Windows.

The per-game override values for `GBTemp_Akalabeth` were **not independently read back or verified**. In particular, this test did not confirm that LaunchBox had saved the intended package, activity, action, data placeholder, or launch arguments. The failure message names **Custom Emulator (With Code)**, but by itself does not prove the exact saved command.

## Documented interface versus device evidence

LaunchBox’s [Custom Emulator (With Code) guide](https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code) documents Android intent command syntax including `-n package/activity`, `-a action`, and `-d {file.uri}` or `-d {file.path}`. It describes the supported command format generally; this failed attempt does not verify that the particular command was saved or accepted on this Odin/LaunchBox 1.23 setup.

The intended GameBridge command supplied for this test was:

```text
-n com.allourmedia.gamebridge.debug/com.allourmedia.gamebridge.BridgeActivity -a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}
```

The exact command remains **unverified on-device**. The prior report’s successful content-URI dispatch was to `dev.launchboxprobe/.ProbeActivity` using a separate diagnostic test and must not be reported as a GameBridge G1 success.

## Current state and rollback

- LaunchBox currently has the one test record `GBTemp_Akalabeth` under Windows.
- The temporary file `/storage/emulated/0/Download/gamebridge-g1/GBTemp_Akalabeth.gog` remains.
- GameBridge-filtered logcat was started and remained without matching output during observation (ADB session `36303` at report time).
- The pre-test backup is `<backup folder>/`; its checksum manifest is `SHA256SUMS`.
- The original `Games/pc/Akalabeth_ World of Doom.gog` was not modified by this test. The original LaunchBox configuration can be restored from the user-created backup if desired, but doing so would remove the test record and discard any changes made after that backup.
- No cleanup was done because the diagnostic entry and shortcut are still the active test setup.

## Next safe step

Locate and inspect the per-game **Custom Emulator (With Code)** settings for `GBTemp_Akalabeth`, then verify the saved command exactly matches the intended component/action/data command above. Before another launch attempt, confirm the GameBridge package/activity is resolvable on-device. Keep `adb logcat -s GameBridge` running, launch only the temporary entry, and capture both LaunchBox’s result and any receiver log. Do not grant Calendar file access. If the per-game fields remain inaccessible, stop and document the exact UI screen or obtain an export/readback method rather than guessing at configuration XML.

## References

- [LaunchBox Android getting started](https://feedback.launchbox-app.com/articles/1026472-getting-started-with-launchbox-for-android)
- [LaunchBox Custom Emulator (With Code)](https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code)
- [Earlier LaunchBox → GameNative investigation](launchbox-gamenative-investigation-20261101.md)
