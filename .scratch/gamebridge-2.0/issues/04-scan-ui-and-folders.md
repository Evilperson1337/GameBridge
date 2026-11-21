# 04: Setup screen folder grants and Scan

**What to build:** In `:app`, SAF grants for the GameNative Shortcut Folder, the Artemis Shortcut Folder and the Output Folder (persisted, read-only for the first two); a manual Scan button that feeds the planner and writes files; a result summary (added, skipped, rejected, found-not-enabled) and a confirm step for any overwrite. No `MANAGE_EXTERNAL_STORAGE`, network or background work.

**Blocked by:** 02, 03

**Status:** needs-triage

- [ ] Granting, re-granting and revoking a folder behaves sensibly.
- [ ] Scan never writes outside the Output Folder.
- [ ] The setup screen shows "verified with <app> <version>" per target from the evidence gate.
