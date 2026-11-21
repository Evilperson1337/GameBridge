# 02: Scan planner in :core

**What to build:** A pure function: given the names and contents of files found in each Target App's Shortcut Folder, and the names already in the Output Folder, return files to write, files skipped, and files rejected with reasons. Rules: file name stem reused; Target App suffix on cross-target collision; existing output files are never overwritten (reported as needing confirmation); unverified targets reported "found, not enabled"; GameNative `0` and non-numeric IDs rejected; Artemis files missing both UUIDs rejected; unknown extra files (for example `.droiddeck-sync.json`, `images/`) ignored. Test-first.

**Blocked by:** 01

**Status:** needs-triage

- [ ] Tests cover each rule above.
- [ ] Summary text names files and reasons, never file contents.
- [ ] No Android imports in `:core`.
