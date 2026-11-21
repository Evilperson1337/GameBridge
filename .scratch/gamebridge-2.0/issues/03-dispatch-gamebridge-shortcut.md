# 03: Dispatch a `.gamebridge` Shortcut

**What to build:** Add a `.gamebridge` branch to `BridgeActivity` next to `.art` and the Local Shortcut extensions. Parse via `:core`, then Dispatch with the fixed template for the named Target App, reusing the existing GameNative and Artemis dispatchers. Missing or refusing Target App uses the existing failure dialogs; no fallback to another target or a deep link. The legacy path stays unchanged. No new exported component, permission or service.

**Blocked by:** 01

**Status:** needs-triage

- [ ] Instrumented test: a `.gamebridge` content URI yields the explicit intent for each target.
- [ ] Legacy `.steam`, `.art` etc. behaviour tests still pass.
- [ ] Artemis: if the launch needs a `.art` URI, stop and record the question (spec, open questions); do not invent a fallback.
- [ ] Release logging records only the failure category.
