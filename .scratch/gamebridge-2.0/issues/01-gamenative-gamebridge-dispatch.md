# 01: GameNative `.gamebridge` Dispatch

**What to build:** A hand-written GameNative Bridge Shortcut, handed to the existing GameBridge entry point, is parsed, validated and Dispatched to GameNative with a fixed template, or fails with an explicit dialog. The format follows ADR 0002 (UTF-8 JSON, at most 4,096 bytes, `version`, `target`, `storefront`, Store ID; unknown fields, targets and Storefronts rejected; unknown newer `version` is a distinct failure). Store IDs are positive signed 32-bit and rejected, never truncated. Legacy native Shortcuts behave exactly as before. No new exported component, permission or service. Parser and validator go in `:core` with tests written first.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `:core` tests: round trip; rejection of oversize, bad JSON, unknown fields/target/Storefront, newer version, out-of-range and non-positive IDs.
- [ ] Failure messages are pinned as literals in tests.
- [ ] Instrumented test: a `.gamebridge` content URI yields the explicit GameNative intent; missing or refusing GameNative gives the existing dialogs; no fallback.
- [ ] Legacy `.steam`/`.epic`/`.gog`/`.amazon`/`.pcgame`/`.art` tests still pass.
- [ ] Release logging records only the failure category.
- [ ] Debug build Dispatch from adb on a temporary copy is recorded in `docs/investigation-log.md` as a Dispatch, not a Launch.
