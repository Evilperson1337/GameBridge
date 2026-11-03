# Spec: Artemis Dispatch

**Status:** implemented (tickets 01 to 07 resolved; see `docs/investigation-log.md` for what is verified)

**Source:** a conversation on 2026-11-02 after the Odin showed Artemis (`com.limelight.noir` 20.2.6) accepting a VIEW of a `.art` file (see `docs/investigation-log.md`, entries "Artemis entry points on the Odin", ".art file format" and "VIEW of a .art file sent to Artemis from adb"). Vocabulary follows `GLOSSARY.md`. This is the first time Artemis support is in scope; the handoff's Phase 3 (discovery, matching, picker) stays out.

## Problem Statement

LaunchBox games that are Remote Shortcuts (`.art` files exported by Artemis) can't be played through GameBridge. Picking one in LaunchBox reaches GameBridge, which, when this spec was written, only knew Local Shortcuts and showed a "Remote Shortcuts aren't supported yet" failure (historical; since replaced by the Dispatch to Artemis).

## Solution

When LaunchBox hands GameBridge a `.art` Remote Shortcut, GameBridge checks it is a `content://` URI with the `.art` extension and Dispatches it to Artemis as an explicit `ACTION_VIEW` with a read grant on the same URI. GameBridge does not read or parse the file: Artemis owns the format. If Artemis isn't installed or refuses, the failure dialog says so. Local Shortcuts behave exactly as before.

## User Stories

1. As a player, I want picking a `.art` game in LaunchBox to open it in Artemis, so that streamed games work from the same library.
2. As a player, I want the same Setup command to work for `.art` and Local Shortcuts, so that I configure LaunchBox once.
3. As a player, I want a clear failure dialog when Artemis isn't installed, so that I know what to install.
4. As a player, I want a clear failure dialog when Artemis refuses the request, so that I know the problem is not LaunchBox.
5. As a player, I want GameBridge never to fall back to GameNative or a deep link for a `.art` file, so that failures are honest.
6. As a maintainer, I want GameBridge to Dispatch without reading the `.art` file, so that it doesn't encode a format built from one sample.
7. As a maintainer, I want the README support table to say Remote Shortcuts are enabled-untested until a device observation is recorded, so that the label is honest.
8. As a maintainer, I want the log to separate the Dispatch (observable) from the stream starting (observed by a person), so that no assumption is presented as verified.

## Implementation Decisions

- **No parsing.** GameBridge validates the URI only: scheme `content` and extension `.art` (case rules as for other extensions). It reads only the display name. Because the file is never opened, the read-size cap does not apply (decided in review; Artemis owns the file). It does not open the `.art` file for any other purpose.
- **Dispatch.** An explicit `ACTION_VIEW` to Artemis's `ShortcutTrampoline` component, with the Shortcut's `content://` URI as data and `FLAG_GRANT_READ_URI_PERMISSION`. Package `com.limelight.noir` (the only observed Artemis package). The component and package are constants in the app module, with a note that the contract was observed on 20.2.6 only.
- **Package visibility.** Add `com.limelight.noir` to `<queries>`. No permission, network, service or background work.
- **Failures.** A new `FailureCategory` for Artemis not installed and one for Artemis refusing, using existing failure-dialog structure. `RemoteUnsupported` is removed or repurposed; the README troubleshooting table is updated to the exact strings. There is no Storefront for a `.art` failure.
- **Target App.** The glossary already names Artemis as the Target App for Remote Shortcuts. No glossary change expected.
- **Fixtures.** Committed `.art` fixtures use placeholder host and app values, never the maintainer's real UUIDs or host name. Real `.art` samples (for example `Desktop.art`) stay uncommitted; `docs/research/*.art` is gitignored.
- **Support table.** Remote Shortcuts (`.art`) row: enabled-untested until ticket 03 records a device observation.

## Testing Decisions

- **Seam 1, `:core` JVM tests.** Classification of a URI and filename as a Remote Shortcut; rejection of non-`content://` URIs and wrong extensions; failure reasons pinned as literals.
- **Seam 2, `:app` instrumented tests** (existing bridge activity test style). A `.art` content URI produces an explicit VIEW to the Artemis component with the grant flag and the same data URI, asserted with a fake Target App registered or an intent capture; Artemis absent produces the not-installed dialog; local Shortcut behaviour is unchanged.
- **Procedure.** The real Dispatch from LaunchBox to Artemis is a device observation (ticket 03), including what Artemis does with a missing or bad file, using temporary copies only.

## Out of Scope

- Reading or parsing `.art` contents; host or app matching; discovery; the local/remote picker (Phase 3).
- Any Artemis variant other than `com.limelight.noir`.
- Starting or waking the PC, or reporting whether the stream started.
- Changes to how Local Shortcuts are read, validated or dispatched.

## Further Notes

- The adb test used `file://` as the shell user. The `content://` plus grant path is untested; ticket 03 settles it. If Artemis ignores the grant, stop and revisit this spec before building a workaround.
