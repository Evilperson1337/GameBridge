# 07: Installed apps as tiles on the setup screen

**What to build:** The setup screen shows the three apps it checks (GameNative, Artemis, LaunchBox) as bounded tiles, in the style of the EmuDeck emulator picker: each tile is a rounded box holding the app's logo, its name, and its installed status. This replaces the three plain status lines from ticket 05; the readiness line above them (ticket 06) stays.

Logo: the installed app's own launcher icon, read with `PackageManager.getApplicationIcon` (package visibility is already declared for all three). Nothing is bundled and nothing is fetched. A not-installed app has no icon to read, so its tile shows a neutral placeholder (the app's first letter on a flat tone step) and its text colour is the muted ink. Name: the fixed names GameNative, Artemis, LaunchBox (glossary terms), not the package label. Status: "Installed" or "Not installed" as text; "Not installed" uses the negative colour and is the only coloured text. The tile is bounded by a rounded shape on a tone step above the ground, with a 1dp edge; no shadow.

Layout: tiles sit in a row that wraps (three across when wide, stacking or wrapping on a narrow screen), in the left column or the top section of the existing layout from ticket 06. Each tile is at least 96dp wide and 48dp tall touch-size is not required (tiles are not interactive) but text stays at the screen's existing sizes. Each tile exposes one content description that reads like the old line ("GameNative: installed") so the accessibility reading is unchanged.

Constraints: no new permission, network access, service or background work; no link-out or version check; tiles do nothing when tapped; the screen dispatches nothing. Keep the hand-built View approach and no new dependencies. Pure text logic stays in `:core` (reuse `appStatusLine` or split it into name and status, with JVM tests).

**Blocked by:** None (tickets 05 and 06 are merged)

**Status:** resolved

- [x] JVM tests cover the name and status text for installed and not-installed, and the content-description text, pinned as literals.
- [x] An instrumented test shows one tile per app with its name and its status text, an installed case (use the test app's own package or a registered stub) showing a real icon view with a non-null drawable, and a not-installed case showing the placeholder letter.
- [x] Existing setup-screen tests for the command text, clipboard content, checklist, readiness line and manifest cross-check still pass (updated only where the plain status lines became tiles).
- [x] Screenshots of the screen in landscape, in light and dark, saved to the scratchpad and read; any problem seen is fixed. If a physical Odin check is made, use `adb -s <serial>` with the serial from `adb devices` and a standing-approved install of the debug build only; say what was and was not seen. Portrait stays unverified unless actually seen.
- [x] No new permission, network access, service or background work; no link-out or version check; tiles dispatch nothing.
- [x] `./gradlew check` and the instrumentation task pass.

## Verification notes

- JVM tests, `./gradlew check` and `:app:pixel2api33DebugAndroidTest` (48 tests, 0 failures) all run and pass.
- Screenshots: emulator landscape light and dark (plus portrait light) were read. Tiles first wrapped and clipped; fixed by tighter padding. Only the not-installed state was seen on screen. The Odin was not used. Details in `docs/investigation-log.md`.
