# 07: DroidDeck target (slice 2)

**What to build:** Pin DroidDeck's contract on a device first: installed version, whether SAF can read its single mixed export folder, its ID range, and its launch intent (`com.droiddeck.launcher/.FrontEndActivity`, `app_id` as String, optional `game_source`) as recorded in `docs/gamebridge-2.0/04-EXPORT-FORMATS-AND-FOLDERS.md`. Then add the target, validator and scan rules. The scanner must ignore `.droiddeck-sync.json` and `images/`.

**Blocked by:** 05

**Status:** needs-triage

- [ ] Device evidence recorded before the `verified` flag is set.
- [ ] Per-target ID range used; DroidDeck IDs beyond signed 32-bit are valid only for this target.
