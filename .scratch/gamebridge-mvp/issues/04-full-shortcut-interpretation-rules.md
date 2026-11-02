# 04: Full Shortcut interpretation rules

**What to build:** Every Shortcut GameBridge receives is either dispatched or rejected with a specific, player-readable reason. The Storefront table gains its remaining rows, so every Local Shortcut GameNative exports (`.gog`, `.steam`, `.epic`, `.amazon`, `.pcgame`) is parsed through the same convention, but only GOG is dispatched:
- Steam, Epic, Amazon and Custom report that support isn't verified yet.
- `.art` reports that Remote Shortcuts aren't supported yet.
- Unknown extensions report "Not a supported Shortcut".
- Malformed, out-of-range and oversized Shortcuts are refused with clear reasons.

See the spec's "Storefront table", "Shortcut parsing rules" and "Testing Decisions" sections.

**Blocked by:** 03 (Error dialog for intent, read and dispatch failures)

**Status:** ready-for-agent

- [ ] The Storefront table has all five rows from the spec: GOG (verified), and Steam, Epic, Amazon and Custom (`.pcgame`, `game_source` `CUSTOM_GAME`), all unverified. No Storefront-specific code exists outside the table.
- [ ] A JVM test shows that flipping a row to verified is enough to produce a dispatch request for that Storefront, with its `game_source` value.
- [ ] Extensions match the table case-insensitively, plus `.art` → Remote Shortcut. Anything else is unsupported.
- [ ] Local Shortcut contents are decoded as UTF-8 with an optional BOM stripped and surrounding whitespace, CR/LF and blank lines trimmed. They must be exactly one positive decimal integer within signed 32-bit range. Zero, negative, out-of-range, non-numeric, multi-number and empty contents are rejected as invalid, never truncated.
- [ ] Shortcuts over 4 KiB are rejected as too large, and reading stops at the cap without reading the whole file.
- [ ] `.steam`, `.epic`, `.amazon` and `.pcgame` with valid contents produce the "Storefront not verified yet" rejection, naming the Storefront. `.art` produces the "Remote Shortcut unsupported" rejection.
- [ ] Fixtures are added:
  - `Broforce.gog` (1470490225), `Broforce.steam` (274190), `ABZU.epic` (64)
  - one synthetic `.amazon` and one synthetic `.pcgame`, in GameNative's format (an Int with no newline)
  - synthetic: BOM, CRLF, blank lines, zero, negative, above 2³¹−1, non-numeric, two numbers, empty, oversized, unknown extension, upper-case extension

  No `.art` fixture content is invented.
- [ ] JVM tests at the interpret-a-Shortcut seam cover every rule and fixture.
- [ ] Each rejection category is shown through the error dialog with its own reason, with at least one instrumentation test per category.
- [ ] `./gradlew check` and the instrumentation suite pass.
