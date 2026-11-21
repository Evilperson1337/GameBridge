# 06: Slice 1 device validation and evidence flags

**What to build:** On the Odin, record what makes each target trustworthy and flip the `verified` flags. Needs explicit approval first; follow `docs/device-testing.md`; temporary copies only.

**Blocked by:** 02, 03, 05

**Status:** ready-for-human

- [ ] SAF can read the real GameNative and Artemis export folders, or the limit is recorded.
- [ ] Installed app versions, GameNative flavor, and Android 14 to 16 behaviour (or "not tested") recorded.
- [ ] Dispatch (logcat) and Launch (person) recorded separately in `docs/investigation-log.md`.
- [ ] `verified` flags flip only after those entries; the setup screen shows "verified with <app> <version>".
