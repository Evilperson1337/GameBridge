# 01: Bridge Shortcut format in :core

**What to build:** A parser and writer for `.gamebridge` in `:core` per ADR 0002: UTF-8 JSON, at most 4,096 bytes, `version`, `target`, `storefront`, IDs. Per-target validators for GameNative (positive signed 32-bit, no truncation) and Artemis (`host_uuid`, `host_name`, `app_uuid`, `app_name`, length-capped, no control characters). Unknown fields, targets and Storefronts are rejected; an unknown newer `version` is a distinct failure. Write tests first.

**Blocked by:** none (the Artemis field list is provisional until 05)

**Status:** needs-triage

- [ ] Round-trip tests for each target; rejection tests for oversize, bad JSON, unknown fields/target/storefront, newer version.
- [ ] Out-of-range and zero/negative GameNative IDs are rejected, not truncated.
- [ ] Failure categories have literal message tests.
- [ ] No Android imports in `:core`.
