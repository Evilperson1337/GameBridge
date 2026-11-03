# 05: Setup screen reports Artemis and LaunchBox

**What to build:** The setup screen already says whether GameNative is installed. It also says whether Artemis (`com.limelight.noir`) and LaunchBox (`com.unbrokensoftware.launchbox`) are installed, in the same plain text style ("Artemis: installed" / "Artemis: not installed", and the same for LaunchBox), so a player finds out before they try to play. Text only: no link-out, no version check, no launching. Package visibility is added for LaunchBox in the manifest `<queries>` (Artemis is already there). The package names are constants next to the existing GameNative one. No permission, network, service or background work.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] Instrumented tests show each of the three apps reported as installed or not installed independently, including an installed case for at least one app (use a real installed package such as the test app's own or a stub package that is registered, and say how).
- [x] The setup screen text for GameNative is unchanged.
- [x] `<queries>` lists LaunchBox; no new permission, network access, service or background work; no link-out or version check.
- [x] The README and `docs/launchbox-setup.md` setup-screen description mention the extra lines if they describe the screen.
- [x] `./gradlew check` and the instrumentation task pass.

## Verification notes

- Criterion 1: `AppStatusTest` (JVM) covers the text for installed and not installed. `SetupActivityTest.installedCheckSeesARegisteredPackageAndNotAMissingOne` uses the test app's own registered package as the installed case and a nonexistent one as the missing case, through the same `isPackageInstalled` the screen uses. `reportsArtemisAndLaunchBoxEachByTheirOwnInstalledState` checks each line on screen. On the emulator neither Artemis nor LaunchBox is installed, so the on-screen "installed" text for those two is covered only at the helper and text-function level.
- Criterion 2: the old inline text is now `appStatusLine("GameNative", ...)`; `AppStatusTest` pins both strings, and the existing `reportsGameNativeAbsentWhenItIsNotInstalled` still passes.
- Criteria 3 to 5: diff reviewed (manifest adds one `<package>`; no new permission or service); README and `docs/launchbox-setup.md` updated; `./gradlew check pixel2api33DebugAndroidTest` passed (SetupActivityTest 9 run, 0 skipped).
