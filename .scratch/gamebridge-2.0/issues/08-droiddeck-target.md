# 08: DroidDeck target (slice 2)

**What to build:** Pin DroidDeck's contract on a device first (installed version, whether SAF can read its single mixed export folder, ID range, launch intent), then add the target, validator and scan rules. Scan ignores `.droiddeck-sync.json` and `images/`. DroidDeck IDs beyond signed 32-bit are valid only for this target.

**Blocked by:** 06

**Status:** needs-triage

- [ ] Device evidence recorded before the `verified` flag is set.
- [ ] Per-target ID range used; GameNative's signed 32-bit rule unchanged.
