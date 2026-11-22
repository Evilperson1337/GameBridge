# Coding standards

Rules for review. Mechanical rules live in the build instead: ktlint, Android lint and the tests run by `./gradlew check` (CI runs it on every push).

## Evidence

Every claim of device behaviour in README, `docs/` (outside the log itself) or a ticket checkbox matches an entry in `docs/investigation-log.md` with the same scope: the same app and version, the same count ("two of five", not "five"), the same path (adb versus a LaunchBox pick), and the same outcome (Dispatch versus Launch). A ticked checkbox needs that entry too, for every part of the box ("set and unset" needs both). A claim with no matching entry is labelled assumed or not tested, in those words.

## Dispatch is not Launch

Names, log lines, UI text and docs say Dispatch for a successful `startActivity` and Launch only for a game or stream a person saw start. `DispatchResult`, never `LaunchResult`.
