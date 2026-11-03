# 06: Setup screen redesign

**What to build:** The setup screen is redesigned for the Odin (1920×1080, landscape, controller-driven) and still works as one column on a phone. Its one question is "What do I paste into LaunchBox, and is everything ready?" Read the review in the conversation record below and `SetupActivity.kt` after ticket 05 has merged.

Layout, landscape: a left column with the title "GameBridge", one readiness line that states the finding ("GameNative, Artemis and LaunchBox are installed", or names what is missing), and the three status lines from ticket 05. A right column with the three checklist steps and, in step 2, the Setup command in a monospace block on a tone step (no border, no shadow) with Copy joined to its end. On a narrow screen it stacks as one column in the same order.

Behaviour: Copy puts exactly the Setup command on the clipboard as before and its label changes to "Copied" for about two seconds, then returns. Copy holds focus when the screen opens, shows a visible focus state, and every touch target is at least 48dp. "Not installed" is the only coloured text (the negative colour); everything else is ink and grey. Two text sizes plus the title, one monospace for the command only, flat colours that follow the system light or dark setting, one accent on the Copy button. No new permission, network access, service or background work; no link-out or version check; the screen dispatches nothing.

**Design rules** (from `.claude/skills/design-taste-frontend/references/app-screens.md`, app-screen rules; its web inspector does not apply to a native Activity): colour goes on status and the one action only; no icons in tinted circles; no shadows; sentence case; text reaches 4.5:1 on its surface in both themes; one gutter value; the command block's right edge and Copy share one edge.

**Blocked by:** 05 (Setup screen reports Artemis and LaunchBox)

**Status:** ready-for-agent

- [ ] The readiness line is built by a pure function in `:core` from the three installed flags, with JVM tests for all-installed, one missing, several missing, and none installed, each naming exactly the missing apps.
- [ ] An instrumented test shows Copy still places exactly the Setup command on the clipboard, and its label reads "Copied" afterwards and returns to "Copy".
- [ ] An instrumented test shows Copy has focus when the screen opens and every interactive view is at least 48dp.
- [ ] Existing setup-screen tests for the command text, GameNative present or absent, the checklist, and the manifest cross-check still pass (updated only where the layout moved).
- [ ] The screen is checked in landscape (1920×1080) and portrait on the managed emulator by saving screenshots to the scratchpad and reading them, in light and dark; any problem seen is fixed. A look on the physical Odin is left to the maintainer and noted as not verified if it was not done.
- [ ] No new permission, network access, service or background work; no link-out or version check; the screen dispatches nothing.
- [ ] `./gradlew check` and the instrumentation task pass.
