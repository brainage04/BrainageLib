# Test review

## Frozen pruning rubric (2026-10-19)

Classification used `judge_batch` with the same rubric for every original test, before manually reading the flagged tests:

- Keep: asserts production behavior visible to players/server owners or a meaningful library contract, and can catch a plausible bug. Cross-loader runs of a behavioral body are not redundant. Keep uncertain cases.
- Redundant: every meaningful assertion is covered at the same or stronger boundary by another named test. Partial overlap is not enough.
- Tautological: asserts constants, copied metadata, mock echoes, harness settings, or registration/command-tree wiring without executing meaningful behavior. Screenshots and pauses are not output assertions.

## Original test decisions

- Keep `aggregatesAndSortsInstalledModHelp`: verifies registry aggregation and ordering.
- Keep `unregisterRemovesOnlyTheExactRegisteredEntry`: verifies registry removal.
- Remove `fabricLoaderBootsInServerModeForTests`: only checks the harness environment.
- Remove `fabricLoaderCanResolveTheTemplateModMetadata`: compares loaded metadata with constants and checks that metadata exists.
- Remove `initializeRegistersServerModsCommands`: only checks command-tree nodes, never executes a command.
- Remove `serverModsCommandIsRegistered` on Fabric and NeoForge, with its adapters and test-instance resource: only checks the command-tree root.
- Remove the original `BrainageLibClientGameTest.runTest`: asserts command-tree/fixture wiring, then records commands with fixed pauses but never asserts their chat output.

No original test was classified as redundant. The two retained tests were not manually reread during pruning.

## Coverage gaps and replacements

Before this wave, execution/output of help and config commands, permission denial, feedback formatting/delivery, first-join persistence, and actual client chat had no behavioral coverage.

- Unit tests now cover duplicate/conflicting registrations, immutable snapshots, normalized/invalid help entries, operator-only first-join hints, and feedback component copying/styles.
- Shared server GameTests execute the actual loader-registered commands, check player/operator output and denial, deliver all three feedback categories, and exercise notice deduplication, disk persistence, reload and clearing. Each body runs on both Fabric and NeoForge, in development and against release JARs.
- The Fabric client scenario checks real login chat, repeated join suppression, help output, denied and permitted config output, and all three player-feedback methods. It waits for responses rather than assuming a fixed delay is enough.
- Fixtures unregister in `finally`; persistence tests preserve and restore the world's previous acknowledgement file.

There are no gamerules, configuration screen, or rendering features beyond chat. Remaining manual/integration gaps: audible UI click playback, NeoForge client login/chat, and a real process restart between joins (the server test reloads the persisted file in one process).

## Code review (2026-10-19)

Reviewed all common production classes and both loader entrypoints. No confirmed production bug was found, and the public API is unchanged.

- Both loaders register the same command, server start/stopping/stopped callbacks, and player-join handler.
- Registry mutation and first-join state are synchronized; command output uses registry snapshots. No background executor or shutdown resource was introduced.
- State readers/writers use try-with-resources, and replacement uses an atomic move where supported.
- Help and feedback are intentionally English literals; there are no untranslated gamerule/config keys.

Judgement calls left unchanged: the saved state's schema version is written but not checked on load; unknown-version handling needs a policy if the format changes. State is process-global and assumes Minecraft's usual single active server per JVM. UI click playback and NeoForge client presentation still need integration coverage.
