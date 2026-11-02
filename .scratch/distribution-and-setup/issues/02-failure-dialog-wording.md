# 02: Failure dialog wording

**What to build:** A player who sees a failure dialog is told what to change in LaunchBox, not pointed at a repo document they do not have. Reasons that currently say to check a command in the repo docs are reworded (for example, the command must end with the Shortcut placeholder as a content URI). The dialog is otherwise unchanged: title, filename when known, one reason, one Close button that returns to LaunchBox. The README troubleshooting table is updated to the exact new strings.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] No failure reason refers to a repo file or a document the player would not have.
- [x] The existing failure tests are updated to pin the new wording, and every reason is still covered.
- [ ] The dialog still shows one Close button and Close or Back returns to LaunchBox.
- [ ] The README troubleshooting table matches the dialog strings exactly.
- [x] `./gradlew check` passes.

## Notes (implementation)

- Criterion 1 checked by `BridgeFailureTest.noReasonPointsAtARepoFile` (no `docs/` or `.md` in any reason) plus reading every string. Criterion 2: `everyReasonIsPinnedAndTellsThePlayerWhatToChange` pins all 11 reasons as literals and asserts the map covers every `FailureCategory`. Criterion 5: `ANDROID_HOME=$ANDROID_HOME ./gradlew check --offline` passed (JVM tests, ktlint, lint). `check` does not run instrumented tests.
- Criterion 3 left unticked: dialog code is unchanged and `BridgeActivityTest` reads `failure.message` from `:core`, but instrumented tests were not run (no emulator or device here).
- Criterion 4 left unticked: the README troubleshooting table is rewritten in ticket 05. The new exact strings are in `FailureCategory` in `BridgeFailure.kt`.
- Status left as `ready-for-agent` because criteria 3 and 4 are open.
