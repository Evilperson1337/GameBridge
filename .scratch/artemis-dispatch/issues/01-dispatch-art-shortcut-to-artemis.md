# 01: Dispatch a .art Shortcut to Artemis

**What to build:** A `.art` Remote Shortcut handed to GameBridge by LaunchBox is validated (content URI, `.art` extension, size cap) and Dispatched to Artemis as an explicit `ACTION_VIEW` on the same URI with a read grant. GameBridge does not parse the file. Local Shortcuts are unchanged. See the spec's "Implementation Decisions" and "Testing Decisions".

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] JVM tests show a `content://` URI ending `.art` is classified as a Remote Shortcut, and non-content URIs or other extensions are rejected as before.
- [ ] An instrumented test shows a `.art` content URI produces an explicit VIEW to Artemis's `ShortcutTrampoline` with the same data URI and the read-grant flag.
- [ ] `<queries>` lists `com.limelight.noir`; no new permission, network, service or background work.
- [ ] A Local Shortcut Dispatch to GameNative is unchanged and its existing tests pass.
- [ ] No fallback to GameNative or a deep link when Artemis cannot be used.
- [ ] `./gradlew check` and the instrumentation task pass.
