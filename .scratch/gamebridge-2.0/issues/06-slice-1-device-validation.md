# 06: Slice 1 device validation and evidence flags

**What to build:** On the Odin, record what makes each target trustworthy and flip the `verified` flags. Needs explicit approval first; follow `docs/device-testing.md`; temporary copies only.

**Blocked by:** 02, 03, 05

**Status:** resolved

- [x] SAF can read the real GameNative and Artemis export folders, or the limit is recorded. (GameNative on the SD card: read OK on 2026-11-22, ticket 03.)
- [x] Revoking a folder grant and choosing the folder again behave sensibly, and a rescan reports "unchanged".
- [x] Installed app versions, GameNative flavor, and Android 14 to 16 behaviour (or "not tested") recorded. (Android 13 only; 14 to 16 not tested.)
- [x] Dispatch (logcat) and Launch (person) recorded separately in `docs/investigation-log.md`.
- [x] Ariane: a stream started from a `.gamebridge` file through GameBridge, with its version recorded.
- [x] `verified` flags flip only after those entries; the setup screen shows "verified with <app> <version>".
