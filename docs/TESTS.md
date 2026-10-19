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
