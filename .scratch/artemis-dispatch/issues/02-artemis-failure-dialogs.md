# 02: Artemis failure dialogs

**What to build:** A player sees a clear failure dialog when Artemis is not installed or refuses the request, in the same style as the GameNative failures. The old "Remote Shortcuts aren't supported yet" reason is removed or repurposed, and the README troubleshooting table matches the dialog strings exactly. There is no Storefront in these failures.

**Blocked by:** 01 (Dispatch a .art Shortcut to Artemis)

**Status:** resolved

- [x] JVM tests pin the exact reasons for Artemis not installed and Artemis refused, and every `FailureCategory` is still covered.
  - Checked: `BridgeFailureTest.everyReasonIsPinnedAndTellsThePlayerWhatToChange` pins both Artemis reasons as literals and asserts the map covers `FailureCategory.entries`. `RemoteUnsupported` no longer exists in code (only a historical log line).
- [x] An instrumented test shows the not-installed dialog when Artemis is absent, with one Close button.
  - Checked: `artShortcutWithArtemisAbsentShowsNotInstalledAndNeverFallsBack` runs unstubbed on the managed emulator (no Artemis) and passed; `assertDialog` now also asserts the other two dialog buttons are not displayed, so Close is the only button.
- [x] No reason refers to a repo document, and glossary terms are used.
  - Checked: `noReasonPointsAtARepoFile` passes; the Artemis reasons use Remote Shortcuts and Artemis only, with no Storefront.
- [x] The README troubleshooting table matches the strings exactly.
  - Checked: new JVM test `readmeTroubleshootingTableListsEveryReasonExactly` finds every reason as a table cell in README.md; the README rows were already identical, so no wording changed.
- [x] `./gradlew check` passes.
  - Checked: `./gradlew check` and `./gradlew :app:pixel2api33DebugAndroidTest` both passed.
