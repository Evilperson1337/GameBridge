# 04: Artemis launch without a `.art` file

**What to build:** A recorded answer on whether Artemis accepts a launch built from `host_uuid` and `app_uuid` with no `.art` file behind it (its intent filter requires a `.art` path; `docs/gamebridge-2.0/04-EXPORT-FORMATS-AND-FOLDERS.md`). If not, the spec is revisited; no fallback is invented. Needs explicit device approval first, temporary copies only, and no change to Artemis.

**Blocked by:** 01

**Status:** resolved

- [x] The intents tried and Artemis's responses are recorded without real UUIDs or host names.
- [x] Behaviour with a stale or unpaired host is recorded.
- [x] Installed Artemis version recorded.
- [x] Conclusion written into the spec's open questions and ADR 0002's Artemis field list.

## Comments

- 2026-11-22: Artemis accepts an extras-only launch: explicit VIEW to `ShortcutTrampoline`, no data, String extras `UUID`, `Name`, `AppUUID` and `AppName` (optionally `AppId` and `HDR`). The stream opened (maintainer). A stale host gives Artemis's own "PC not found" dialog. This is the template for ticket 05, so there is no `.art` re-serving and no fallback. Delivery from an app rather than the shell, and on Android 14+, stays unverified (see the log). Criterion 3 (installed version) is 20.2.6.
