# 02: Artemis failure dialogs

**What to build:** A player sees a clear failure dialog when Artemis is not installed or refuses the request, in the same style as the GameNative failures. The old "Remote Shortcuts aren't supported yet" reason is removed or repurposed, and the README troubleshooting table matches the dialog strings exactly. There is no Storefront in these failures.

**Blocked by:** 01 (Dispatch a .art Shortcut to Artemis)

**Status:** ready-for-agent

- [ ] JVM tests pin the exact reasons for Artemis not installed and Artemis refused, and every `FailureCategory` is still covered.
- [ ] An instrumented test shows the not-installed dialog when Artemis is absent, with one Close button.
- [ ] No reason refers to a repo document, and glossary terms are used.
- [ ] The README troubleshooting table matches the strings exactly.
- [ ] `./gradlew check` passes.
