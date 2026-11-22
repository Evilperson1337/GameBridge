# 03: Scan GameNative into the Output Folder

**What to build:** The player grants the GameNative Shortcut Folder (read-only) and the Output Folder through the Storage Access Framework, taps Scan, and gets a `.gamebridge` file per game found plus a summary of added, skipped, rejected and found-not-enabled files (names and reasons, never contents). Rules per ADR 0003: manual only, never write outside the Output Folder, additive, existing output files never overwritten without confirmation, file name is the source stem, Target App suffix on cross-target collision, `0` and non-numeric IDs rejected, unrelated files ignored. The planner is a pure `:core` function written test-first; SAF and UI stay in `:app`. The files written are ones ticket 01 Dispatches. No `MANAGE_EXTERNAL_STORAGE`, network or background work.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] Planner tests cover each rule above.
- [ ] Granting, re-granting and revoking a folder behaves sensibly.
- [x] Scan on temporary copies produces files that ticket 01's Dispatch accepts.
- [x] Summary text and logs never include file contents or device paths in release builds.

## Comments

- 2026-11-22: planner (seam 1) and `ScanRunner` (seam 2, test DocumentsProvider on the API 33 emulator) are done; see `docs/investigation-log.md`, "Ticket 03". `ScanActivity` opens from a "Scan for games" button on the setup screen. Decided with the maintainer: a Storefront suffix on every file in a within-GameNative collision; a separate non-exported `ScanActivity` because the setup screen is `noHistory`. The grant, re-grant and revoke criterion needs a hand check on a device (approval for the folder writes).
