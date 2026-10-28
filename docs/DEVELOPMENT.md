# Development

## Dependency

BrainageLib is not on Maven Central yet. Consumers build it from a sibling checkout; the coordinates are `io.github.brainage04:brainagelib:1.3.0` for Fabric and `io.github.brainage04:brainagelib-neoforge:1.3.0` for NeoForge.

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib:1.3.0"
    productionRuntimeMods "io.github.brainage04:brainagelib:1.3.0"
}
```

For NeoForge, use the loader-specific artifact:

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib-neoforge:1.3.0"
}
```

Declare the runtime requirement in `fabric.mod.json`:

```json
{
  "depends": {
    "brainagelib": ">=1.3.0"
  }
}
```

For NeoForge, declare `brainagelib` as a required dependency in `META-INF/neoforge.mods.toml`.

For workspace development, MinecraftModConventions' `workspaceDependencies` component resolves BrainageLib from a sibling checkout's `build/local-repo` (written by `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`):

```groovy
workspaceDependencies {
    siblingMaven('BrainageLib') {
        coordinate.set("io.github.brainage04:brainagelib:1.3.0")
        siblingDirectory.set(rootProject.layout.projectDirectory.dir('../BrainageLib'))
    }
}
```

In CI, pass `prepare_siblings: BrainageLib` to the MinecraftModConventions reusable workflows; they clone this repository into `../BrainageLib` and publish it to that local repository before building.

## Command feedback

Create one `ModFeedback` instance per mod and reuse it for player and command-source output:

```java
private static final ModFeedback FEEDBACK = ModFeedback.create("Example Mod");

FEEDBACK.neutral(player, "Current mode: %s", mode);
FEEDBACK.success(source, "Configuration saved.", false);
FEEDBACK.failure(source, "Configuration could not be saved.");
FEEDBACK.click(player);
```

Messages receive a grey `[Example Mod]` prefix and a consistent yellow, green, or red body. The API accepts either components or formatted strings and can send the standard UI click sound directly to a player.

Command-source failures keep the same grey prefix as player errors.

## Combined server help

Register each installed server mod during initialization:

```java
ServerModHelpRegistry.register(new ServerModHelpEntry(
        MOD_ID,
        MOD_NAME,
        "Short player-facing description.",
        "/example help",
        "/example config"
));
```

Use `ServerModHelpEntry.playerOnly(...)` when the mod has no operator configuration command.

## Saved files

`io.github.brainage04.brainagelib.storage` holds the shared persistence pieces. Each mod keeps its own file format, validation and messages.

- `AtomicFiles.write(path, output -> ...)`, `writeText`, `writeString` and `replace(staged, target)` write `<name>.tmp` next to the target and move it over the target atomically, with a plain replacing move where the file system has no atomic move. A failed write deletes the temporary file and leaves the target unchanged.
- `JsonFile<T>` reads and writes one JSON document. `load()` returns `Missing`, `Loaded(value)`, `Invalid(exception)` (not JSON, JSON `null`, or rejected by the decoder) or `IoError(exception)`. `save(value)` returns `SAVED`, `FAILED` or `KEPT_ORIGINAL`.
- `WorldPlayerStore<P>` keeps UUID-keyed player preferences in a world-folder file shaped `{"schemaVersion": N, "players": {"<uuid>": {...}}}`. Call `load(server)` when the server starts (a missing file is created empty) and `close()` when it stops.

A file that failed to load is never overwritten by default (`FailedLoadPolicy.KEEP_ORIGINAL`): changes stay in memory for the session, `save` returns `KEPT_ORIGINAL`, and the mod tells the player or console that the change was not saved. A later successful load allows saves again. Choose `ALLOW_OVERWRITE` only for files that are safe to replace.

```java
private static final WorldPlayerStore<Preference> PLAYERS = WorldPlayerStore
        .builder("example-players.json", Preference.class, Preference::new)
        .description("Example player settings")
        .logger(LOGGER)
        .build();

boolean saved = PLAYERS.save().saved();
```

Load problems, refused saves and write failures are logged by BrainageLib.

## Confirmation windows

`ConfirmationWindow<K>` implements "run the command again to confirm" in server ticks. Use one instance per action, call `requestOrConfirm(key, server.getTickCount())` and act on `CONFIRMED`; send the warning on `REQUESTED`. `Boundary.INCLUSIVE` still confirms exactly `windowTicks` after the request, `Boundary.EXCLUSIVE` does not. `cancel(key)` drops one request, `clear()` all of them (call it when the server stops), and `expire(tick, key -> ...)` drops closed requests so the mod can tell players.

## Drop capture

`io.github.brainage04.brainagelib.drops` sends drops and experience to the player responsible for them, for every mod that installs a policy. BrainageLib's own mixins open a capture scope for:

- a block a player breaks (`ServerPlayerGameMode.destroyBlock`, and `Block.dropResources` with a player as breaker for mods that call `playerDestroy` or `Level.destroyBlock` themselves), including a broken container's contents and the block's experience;
- a mob's death loot and experience, for the player vanilla credits with the kill (`getLastHurtByPlayer` within its 100-tick memory: melee, owned projectiles and potions, a tamed wolf); a killed player's own drops always stay in the world;
- the block drops of an explosion a player caused: lit TNT and its chain, an owned projectile or fireball, a crystal the player hit, and a bed or respawn anchor the player used where it explodes (vanilla names nobody there; the click is credited around the call, so damage, knockback and kill credit stay vanilla's);
- the ender dragon's death-animation experience, for its killer;
- a decaying leaf of a tree a player felled: every log a player breaks is recorded by BrainageLib itself, and a mod that breaks more logs for a player (a whole tree at once) calls `LeafOwners.felled(player, level, logs)` with those; the record covers the leaves those logs held up (vanilla's six leaf steps, loaded chunks only) for one minute, is kept whatever the policies say (they decide where the drops go when the leaf decays), and is forgotten when the server stops.

Inside a scope, an item entity or experience about to spawn goes to the player instead. What fits goes into the inventory (with the `picked_up` statistic), the rest is moved to the player's feet. Experience repairs Mending items first. A dead player, or one in another dimension, gets nothing.

Whether a player gets anything is decided by `DropPolicy` instances registered with `DropPolicies.register(priority, policy)`. For each player the highest-priority policy that `governs(player)` decides alone: `delivers(player)` says whether drops reach them and `accepts(player, stack)` may refuse single stacks, which then stay where the game spawned them. A player no policy governs keeps every drop in the world, so installing BrainageLib alone changes nothing. Use `DropPolicies.DEFAULT_PRIORITY` for a mod that covers everyone and `DropPolicies.MATCH_PRIORITY` for a minigame that owns its players during a match.

```java
DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, new DropPolicy() {
    @Override public boolean governs(ServerPlayer player) { return true; }
    @Override public boolean delivers(ServerPlayer player) { return settings.enabled(player); }
});
```

## GameTest support

`io.github.brainage04:brainagelib-gametest` is a test-only artifact; never ship it or depend on it from main code. It is a small mod on both loaders, so GameTest runs load it next to the test mod:

```groovy
workspaceDependencies {
    siblingMaven('BrainageLibGameTest') {
        coordinate.set("io.github.brainage04:brainagelib-gametest:1.3.0")
        siblingDirectory.set(rootProject.layout.projectDirectory.dir('../BrainageLib'))
    }
}

dependencies {
    gametestImplementation "io.github.brainage04:brainagelib-gametest:1.3.0"
    productionRuntimeMods "io.github.brainage04:brainagelib-gametest:1.3.0"
}
```

Add both lines to the Fabric and the NeoForge project. `productionRuntimeMods` only feeds the production GameTest runs, not the release JAR.

- `GameTestPlayer.join(helper, name)` joins a real player through `PlayerList.placeNewPlayer` with a server packet listener on an in-memory connection. `command("...")` runs a command through the chat-command packet, `messages()`/`messageTexts()`/`received(text)` return the system chat messages the client was sent, and `gameMode`, `markLoaded` and `moveTo` set the player up. Use it with try-with-resources: `close()` removes the player, disconnects the listener and releases the channel.
- `CapturedCommandOutput` is a command source that keeps every message; `source(server)` gives a server command source that reports to it.

## Build and tests

On this workspace, run Gradle through `heavy`:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests
```

The root build produces Fabric and NeoForge JARs in `build/libs` and `common/build/libs/brainagelib-gametest-<version>.jar`; CI invokes `runAllGameTests` to run the Fabric production GameTests and the NeoForge GameTests on a NeoForge server against the release JAR. BrainageLib's own GameTests compile the GameTest support sources directly.

Builds and CI use MinecraftModConventions 3.1.0.
