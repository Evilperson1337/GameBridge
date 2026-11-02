# Spec: Distribution and setup

**Status:** ready-for-agent

**Source:** a grilling session on 2026-11-02 after the LaunchBox → GameBridge → GameNative path was seen working on the Odin (Epic Dispatch and play; platform-wide setup read back from `Windows.xml`, see `docs/investigation-log.md`). Vocabulary follows `GLOSSARY.md`, including the new **Setup command**.

## Problem Statement

GameBridge works, but only its author can install it. A player has to install JDK 21, the Android SDK and Gradle to get an APK, and the release build isn't signed, so Android won't install it. Setting up LaunchBox means typing a long **Setup command** into Custom Emulator (With Code) from a guide that still says the path is unverified. Opening GameBridge from the app drawer shows an error dialog. Several failure dialogs tell the player to read a repo document they don't have. The README leads with build steps and still says Gate G1 hasn't been run.

## Solution

A player on an Android handheld downloads a signed APK from GitHub Releases, installs it, and opens GameBridge once. A setup screen shows the exact Setup command for their build, with a Copy button, whether GameNative is installed, and a three-line checklist. They set the Windows platform's default emulator to Custom Emulator (With Code), paste the command and clear any per-game emulator override. From then on, picking a Shortcut in LaunchBox dispatches it to GameNative as today. Failure dialogs tell the player what to change in LaunchBox. The README is written for players first, with a table of which Storefronts are verified, and developer material moves to its own document.

## User Stories

1. As a player, I want to download a ready-made APK from GitHub Releases, so that I don't need a PC or build tools.
2. As a player, I want the APK to be signed, so that Android installs it without errors.
3. As a player, I want updates to install over the previous version, so that I keep my LaunchBox setup.
4. As a player, I want the README to open with the download link and what GameBridge does, so that I know in a minute whether it is for me.
5. As a player, I want to open GameBridge from the app drawer and see a setup screen, so that I'm not told off with an error.
6. As a player, I want the setup screen to show the exact Setup command for the build I installed, so that I never have to pick between debug and release variants.
7. As a player, I want a Copy button for the Setup command, so that I avoid typing a long command with a handheld controller.
8. As a player, I want the setup screen to say whether GameNative is installed, so that I find out before I try to play.
9. As a player, I want a three-line checklist on the setup screen, so that I remember the LaunchBox steps without leaving the app.
10. As a player, I want the setup screen to do nothing else, so that it never launches or changes anything by itself.
11. As a player, I want a step-by-step LaunchBox guide that matches the setup that really works, so that I don't try screens that don't exist.
12. As a player, I want the guide to say that the Windows platform's default emulator is replaced, so that I understand the effect on other games on that platform.
13. As a player, I want the guide to tell me to clear any per-game emulator override, so that old GameNative entries don't win over the platform setting.
14. As a player, I want a simple "write down or screenshot your current settings" backup step, so that I can undo the change without adb.
15. As a power user, I want the adb and SHA-256 backup procedure in an Advanced section, so that I can do a verified backup when I want one.
16. As a player, I want to know how to undo the setup, so that I can go back to how things were.
17. As a player, I want failure dialogs to say what to change in LaunchBox, so that I can fix a typo in the command without reading repo docs.
18. As a player, I want the failure dialog to keep its single Close button and return me to LaunchBox, so that recovery stays simple.
19. As a player, I want the README troubleshooting table to match the dialog wording exactly, so that I can find my error by reading it.
20. As a player, I want a table of Storefronts marked verified, enabled-untested or not supported, so that I know what to expect for my games.
21. As a player, I want the README to say that a successful Dispatch is not proof the game started, so that I don't expect GameBridge to report a Launch.
22. As a player, I want the known blank screen after exiting an Epic game to be documented, so that I don't think GameBridge broke.
23. As a player, I want the minimum requirements stated (Android version, tested LaunchBox and GameNative versions), so that I know whether my device is covered.
24. As a player, I want to know which files to import into LaunchBox and where GameNative exports them, so that I get Shortcuts into my library.
25. As a player, I want cropped screenshots of the LaunchBox screens in the guide, so that I can find the right fields (added after the text).
26. As a maintainer, I want a tagged release to build, sign and publish an APK automatically, so that releases are repeatable.
27. As a maintainer, I want the signing key held only in GitHub Actions secrets with an offline backup, so that it can't leak through the repo.
28. As a maintainer, I want the build to produce an unsigned APK and pass `./gradlew check` when the secrets are absent, so that contributors and forks aren't blocked.
29. As a maintainer, I want the first release marked as a pre-release until all five Storefronts have device evidence, so that the label is honest.
30. As a maintainer, I want release notes to link to the README support table, so that the status is in one place.
31. As a maintainer, I want an MIT licence in the repo, so that people may use and share the download.
32. As a maintainer, I want developer setup (SDK, emulator, device testing) in `docs/development.md`, so that the README stays user-first.
33. As a maintainer, I want the investigation log to record the new phase and the closing of Gate G1 with what remains unverified, so that no assumption is presented as verified.
34. As a maintainer, I want a test that the Setup command matches the manifest's action and component, so that the screen and the docs cannot drift from the real app.
35. As a maintainer, I want the setup screen to add no permissions, network access, services or background work, so that the app's safety rules still hold.

## Implementation Decisions

- **Phase.** A new "Distribution and setup" phase. Gate G1 is closed on the strength of the Epic observation and the `Windows.xml` readback, with the leftover doubts recorded: GOG not rechecked through LaunchBox, the blank screen after an Epic exit, and the LaunchBox screens used for setup not recorded.
- **Setup command.** A pure function in `:core` takes the application ID and returns the exact command text, using the full activity class name and the action `com.allourmedia.gamebridge.action.DISPATCH` with `-d {file.uri}`. The setup screen builds the command from the app's own application ID, so debug and release builds each show their own.
- **Setup screen.** A separate launcher Activity. It shows the Setup command, a Copy button (clipboard only), GameNative present or absent (text only, using the existing package visibility), and a three-line checklist. It dispatches nothing, adds no permission, network, service or background work, and has no link-out or version check.
- **Bridge activity.** The no-Shortcut case stays a dialog. Its meaning is now "LaunchBox sent something wrong", since drawer launches no longer reach it.
- **Failure reasons.** Reasons that point at repo documents are reworded to say what to change in LaunchBox (for example, the command must end with `-d {file.uri}`). The dialog is otherwise unchanged: title, filename when known, one reason, one Close button. The README troubleshooting table is updated to the exact strings.
- **Release build.** Signing configuration reads from environment variables. When they are absent the release build is unsigned and `./gradlew check` still passes. A GitHub Actions workflow runs on version tags, builds the release APK, signs it and attaches it to a GitHub Release marked pre-release. The release application ID stays `com.allourmedia.gamebridge`.
- **Keystore.** Generated by the maintainer (a `/wizard` script walks them through it and sets the GitHub secrets). It is never committed. The README states that losing it means players must uninstall before updating.
- **Supported setup.** The platform-wide Windows default emulator is the only documented, verified path. Per-game overrides and other platforms are listed as not verified. The guide includes a warning that every game on the Windows platform then goes through GameBridge.
- **Backup guidance.** The main guide asks for a note or screenshot of the current settings, and says GameBridge never edits LaunchBox, so undoing is removing the Custom Emulator. The adb pull and SHA-256 procedure moves to an Advanced section.
- **Storefronts.** All five stay enabled. A README support table marks GOG and Epic verified (with the date of the observation) and Steam, Amazon and Custom enabled-untested. A row moves to verified only with a device observation recorded in the investigation log.
- **Docs layout.** The README is user-first: what it is, download, Setup command, support table, troubleshooting, limitations. Build, SDK, emulator and device testing material moves to `docs/development.md`. The handoff, investigation log and research documents stay where they are.
- **Glossary.** "Setup command" is added (done).
- **Licence.** MIT, copyright the maintainer.
- **Screenshots.** Cropped LaunchBox screens, taken by the maintainer, added in a later ticket. They must not show device identifiers or paths beyond `Games/pc`. The written steps carry the guide until then.
- **ADRs.** None proposed: no decision here is both hard to reverse and surprising.

## Testing Decisions

- A good test checks external behaviour: the text a player sees, what lands on the clipboard, what the activity dispatches. It does not check layout internals or private functions.
- **Seam 1, `:core` JVM tests.** The Setup command builder returns the exact debug and release strings. The reworded failure reasons are pinned by the existing failure tests, so the README table can't drift from them.
- **Seam 2, `:app` instrumented tests** (the existing bridge activity test style, run on the managed emulator). The setup screen shows the command for the installed build, Copy puts that text on the clipboard, and GameNative present or absent is reported. A cross-check asserts the displayed action and component match what the manifest resolves. The no-Shortcut dialog case keeps its test.
- **Procedure, not unit tests.** The release workflow and signing are verified by a tag build that attaches a signed APK, plus an unsigned `./gradlew check` passing without the secrets. The docs and the LaunchBox walkthrough are checked against the device and the `Windows.xml` readback.
- Prior art: the existing core tests for failures, Shortcut interpretation and intent triage, and the bridge activity instrumented test.

## Out of Scope

- Artemis support, discovery, matching, a launcher picker, a Play Store or F-Droid listing.
- Any change to how GameBridge reads, validates or dispatches a Shortcut.
- A version check or link-out for GameNative, an in-app log viewer, a Copy details button.
- The gold GameNative variant (`app.gamenative.gold`).
- Per-game overrides and platforms other than Windows, until they are tested.
- Fixing the blank screen after an Epic exit: it is GameNative's behaviour and is only documented.

## Further Notes

- The existing debug-build flow on the maintainer's device needs its Setup command changed when they move to the release build, because the application ID differs.
- `docs/gamebridge-g1-launchbox-test-report-20261102.md` links to a document that isn't in `docs/`; fix or remove the link while the docs are being tidied.
