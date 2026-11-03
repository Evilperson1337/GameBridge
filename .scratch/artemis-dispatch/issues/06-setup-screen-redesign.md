# 06: Setup screen redesign

**What to build:** The setup screen is redesigned for the Odin, which is a **portrait** device (observed: 1080x1920 at 369dpi; the 1920x1080 seen earlier was an Artemis stream, not the screen), controller-driven. Landscape and tablets must still work. Its one question is "What do I paste into LaunchBox, and is everything ready?" Read `SetupActivity.kt` after ticket 05 has merged.

What the current screen gets wrong (screenshot of the debug build on the Odin, 2026-11-02): the title "GameBridge" appears twice (action bar and body); the command is body text that wraps in three lines and breaks mid-token; Copy is a default full-width grey button with a shadow; the status line sits unmarked between the button and the steps; the bottom 40% is empty.

Layout, portrait (primary): the action bar is the only title (remove the body title). Under it, one readiness line that states the finding ("GameNative, Artemis and LaunchBox are installed", or names what is missing), then the status lines from ticket 05. Then the three steps; step 2 holds the Setup command in a monospace block on a tone step (no border, no shadow), set so it breaks only at spaces or at `/` (one flag per line is acceptable), with Copy joined to its end or directly beneath it at the same edges. Landscape or wide screens may use two columns (status left, steps and command right); portrait is what is verified.

Behaviour: Copy puts exactly the Setup command on the clipboard as before and its label changes to "Copied" for about two seconds, then returns. Copy holds focus when the screen opens, shows a visible focus state, and every touch target is at least 48dp. "Not installed" is the only coloured text (the negative colour); everything else is ink and grey. Two text sizes plus the title, one monospace for the command only, flat colours that follow the system light or dark setting, one accent on the Copy button. No new permission, network access, service or background work; no link-out or version check; the screen dispatches nothing.

**Design rules** (from `.claude/skills/design-taste-frontend/references/app-screens.md`, app-screen rules; its web inspector does not apply to a native Activity): colour goes on status and the one action only; no icons in tinted circles; no shadows; sentence case; text reaches 4.5:1 on its surface in both themes; one gutter value; the command block's right edge and Copy share one edge.

**Blocked by:** 05 (Setup screen reports Artemis and LaunchBox)

**Status:** ready-for-agent

- [ ] The readiness line is built by a pure function in `:core` from the three installed flags, with JVM tests for all-installed, one missing, several missing, and none installed, each naming exactly the missing apps.
- [ ] An instrumented test shows Copy still places exactly the Setup command on the clipboard, and its label reads "Copied" afterwards and returns to "Copy".
- [ ] An instrumented test shows Copy has focus when the screen opens and every interactive view is at least 48dp.
- [ ] Existing setup-screen tests for the command text, GameNative present or absent, the checklist, and the manifest cross-check still pass (updated only where the layout moved).
- [ ] The screen is checked in portrait (1080x1920) first, then landscape, on the managed emulator by saving screenshots to the scratchpad and reading them, in light and dark; any problem seen is fixed. The physical Odin is connected over USB: after the emulator check, install the debug build with `adb install -r` (standing approval), take a screenshot with `adb exec-out screencap -p`, read it, and note what was seen. Use the Odin's serial; another device may be attached.
- [ ] No new permission, network access, service or background work; no link-out or version check; the screen dispatches nothing.
- [ ] `./gradlew check` and the instrumentation task pass.
