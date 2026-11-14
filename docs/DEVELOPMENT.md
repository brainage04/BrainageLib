# Development

## Dependency

BrainageLib is on Maven Central from 1.3.0 onward: `io.github.brainage04:brainagelib` for Fabric and `io.github.brainage04:brainagelib-neoforge` for NeoForge. The test-only `io.github.brainage04:brainagelib-gametest` is not on Maven Central, and consumers build it from a sibling checkout (see [GameTest support](#gametest-support)).

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib:2.0.0"
    productionRuntimeMods "io.github.brainage04:brainagelib:2.0.0"
}
```

For NeoForge, use the loader-specific artifact:

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib-neoforge:2.0.0"
}
```

Declare the runtime requirement in `fabric.mod.json`, capped below the next major version:

```json
{
  "depends": {
    "brainagelib": ">=2.0.0 <3.0.0"
  }
}
```

For NeoForge, declare `brainagelib` as a required dependency in `META-INF/neoforge.mods.toml` with the same cap:

```toml
[[dependencies.examplemod]]
modId="brainagelib"
type="required"
versionRange="[2.0.0,3.0.0)"
ordering="AFTER"
side="BOTH"
```

Use `side="BOTH"`: a client that installs a mod requiring BrainageLib installs BrainageLib too (see the README's Compatibility section), so NeoForge also checks the dependency on a client and shows its missing-dependency screen there.

A major release may remove public API, as 2.0.0 did. With the cap, the loader refuses to start a mod next to a BrainageLib it was not built for, instead of the mod failing with `NoSuchMethodError` during play.

For workspace development, MinecraftModConventions' `workspaceDependencies` component resolves BrainageLib from a sibling checkout's `build/local-repo` (written by `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`):

```groovy
workspaceDependencies {
    siblingMaven('BrainageLib') {
        coordinate.set("io.github.brainage04:brainagelib:2.0.0")
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

A player-only command binds its player with `requirePlayer`, which sends
`ModFeedback.PLAYERS_ONLY_MESSAGE` to any other source and plays the click sound for a player:

```java
ServerPlayer player = FEEDBACK.requirePlayer(source);
if (player == null) return 0;
```

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

`HelpPage` prints a mod's own `/<mod>` page in the same style and registers the root and its `help` child together:

```java
HelpPage.create(FEEDBACK, "example")
        .line("/example - show this page")
        .line("/example <mode> - switch to a mode")
        .line("You can have up to %d modes.", MODES.size())
        .register(dispatcher);
```

The first line defaults to `Player commands:`, and `heading(...)` replaces it. `line(...)` throws `IllegalArgumentException` for a blank message, so a bad page fails when the mod registers it. `show(source)` prints the page wherever it is called and returns 1.

Brigadier merges the page's `/example` root with the mod's own `/example` root. The root registered first keeps its `requires`, and the root registered last decides what `/example` alone runs. Put permission checks on the mod's subcommands, not on its `/example` root, so that they hold in either registration order.

## Saved files

`io.github.brainage04.brainagelib.storage` holds the shared persistence pieces. Each mod keeps its own file format, validation and messages.

- `AtomicFiles.writeString(path, text)` writes `<name>.tmp` next to the target and moves it over the target atomically, with a plain replacing move where the file system has no atomic move. A failed write deletes the temporary file and leaves the target unchanged.
- `JsonFile<T>` reads and writes one JSON document. `load()` returns `Missing`, `Loaded(value)`, `Invalid(exception)` (not JSON, JSON `null`, or rejected by the decoder) or `IoError(exception)`; `lastLoad()` returns the most recent result. `save(value)` returns `SAVED`, `FAILED` or `KEPT_ORIGINAL`.
- `ConfigFile<T>` is a mod's own config: one document, one default value and the value in effect, without repeating the file setup.
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

`ConfigFile<T>` takes the file, the Gson, the type and the default value; a missing file is created by saving the default value, so the default is the whole document. The mod changes the value and calls `save()`:

```java
private static ConfigFile<ExampleConfig> file;

public static void initialize(Path configDirectory) {
    file = ConfigFile.builder(
                    configDirectory.resolve("example.json"), GSON, ExampleConfig.class, new ExampleConfig())
            .description("Example configuration")
            .logger(LOGGER)
            .build();
    file.loadOrCreate();
}

public static ExampleConfig config() {
    return file.value();
}

public static boolean save() {
    return file.save();
}

public static boolean reload() {
    return file.reload();
}
```

`loadOrCreate()` replaces the value when the document loads and writes the default document when the file is missing. `reload()` only replaces the value, so a deleted file stays deleted. Add `.decoder(...)` for validation, normalisation or migration.

Whenever the default becomes the value in effect (before the first load, and when `loadOrCreate()` finds the file missing), the value is a fresh copy of the default made with the Gson. Changing the value therefore never changes the default, and deleting the file and calling `loadOrCreate()` writes the original default again. Saved files end with a line break.

`save()` writes nothing and returns false in two cases, and the change then applies for the session only:

- before the first `loadOrCreate()` or `reload()`, so an early save cannot replace the operator's file with the default;
- after a load that failed, so a file the mod could not read is never overwritten.

BrainageLib logs load problems, refused saves and write failures.

## Confirmation windows

`ConfirmationWindow<K>` implements "run the command again to confirm" in server ticks. Use one instance per action, call `requestOrConfirm(key, server.getTickCount())` and act on `CONFIRMED`; send the warning on `REQUESTED`. `Boundary.INCLUSIVE` still confirms exactly `windowTicks` after the request, `Boundary.EXCLUSIVE` does not. `cancel(key)` drops one request, `clear()` all of them (call it when the server stops), and `expire(tick, key -> ...)` drops closed requests so the mod can tell players.

## Drop capture

`io.github.brainage04.brainagelib.drops` sends drops and experience to the player responsible for them, for every mod that installs a policy. BrainageLib's own mixins open a capture scope for:

- a block a player breaks (`ServerPlayerGameMode.destroyBlock`, and `Block.dropResources` with a player as breaker for mods that call `playerDestroy` or `Level.destroyBlock` themselves), including a broken container's contents and the block's experience;
- a mob's death loot and experience, for the player vanilla credits with the kill, which is what
  `LivingEntity#getKillCredit()` returns: the player who hurt it within the game's 100-tick memory
  (melee, an owned projectile, potion or firework, thorns from armour they wear, a tamed wolf, TNT
  they lit). A hit inside the 100 ticks keeps the credit for a death from anything else, such as a
  fall, lava, the void or another mob's blow; once the game has forgotten the hit, or credits a
  mob instead, the loot and experience stay in the world. A killed player's own drops always stay
  in the world;
- the drops of an item frame, glow item frame, painting, cushion or armour stand a player breaks:
  the decoration's own item and the item it held, or the stand and its equipment. A blast the
  player caused counts too, through the player the blast names. A blast that names nobody (a
  creeper's), a fire and a decoration that lost its support leave the drops in the world, and a
  creative break drops nothing at all;
- a boat, raft or minecart a player breaks, with a chest boat's, chest minecart's or hopper
  minecart's contents. A furnace or TNT minecart has nothing else to hand over; a damage source
  that names no player (lava, a fire, a mob) leaves the item and contents in the world, and a
  creative break discards the vehicle the way it always did;
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

## Connected blocks

`io.github.brainage04.brainagelib.blocks.ConnectedBlocks` finds the blocks connected to one block, for a vein miner or a tree feller. `find(origin, limit, adjacency, member)` walks breadth-first from `origin` through the neighbours `member` accepts and returns up to `limit` of them, nearest first, `origin` left out. `Adjacency` says which neighbours count: `FACES` (6), `FACES_EDGES` (18) or `FACES_EDGES_CORNERS` (26). Neighbours are visited in one fixed order (x, then y, then z offsets from -1 to 1), so a limit always cuts the same far end off, and `member` is asked about each position once. `member` reads the world itself; `ConnectedBlocks.loaded(level, statePredicate)` reads only chunks already loaded and rejects positions outside the build height, so a walk never loads a chunk. Use one `loaded` predicate per walk: it remembers the last chunk it read.

```java
List<BlockPos> vein =
        ConnectedBlocks.find(origin, 63, Adjacency.FACES_EDGES_CORNERS,
                ConnectedBlocks.loaded(level, state -> state.is(Blocks.IRON_ORE)));
```

## GameTest support

`io.github.brainage04:brainagelib-gametest` is a test-only artifact; never ship it or depend on it from main code. It is a small mod on both loaders, so GameTest runs load it next to the test mod:

```groovy
workspaceDependencies {
    siblingMaven('BrainageLibGameTest') {
        coordinate.set("io.github.brainage04:brainagelib-gametest:2.0.0")
        siblingDirectory.set(rootProject.layout.projectDirectory.dir('../BrainageLib'))
    }
}

dependencies {
    gametestImplementation "io.github.brainage04:brainagelib-gametest:2.0.0"
    productionRuntimeMods "io.github.brainage04:brainagelib-gametest:2.0.0"
}
```

Add both lines to the Fabric and the NeoForge project. `productionRuntimeMods` only feeds the production GameTest runs, not the release JAR.

- `GameTestPlayer.join(helper, name)` joins a real player through `PlayerList.placeNewPlayer` with a server packet listener on an in-memory connection. `command("...")` runs a command through the chat-command packet, `messages()`/`messageTexts()`/`received(text)` return the system chat messages the client was sent, and `gameMode`, `markLoaded` and `moveTo` set the player up. Use it with try-with-resources: `close()` removes the player, disconnects the listener and releases the channel.
- `GameTestPlayer.join(level, profile)` joins the same kind of player in a given `ServerLevel`, for tests without a `GameTestHelper`, such as a client GameTest that needs a second player.
- `GameTestPlayer.mock(helper)` and `mock(helper, gameMode)` join a mocked player in creative mode with the name `test-mock-player`, the supported replacement for `GameTestHelper.makeMockServerPlayerInLevel()`; close it like any other fixture.
- `CapturedCommandOutput` is a command source that keeps every message; `source(server)` gives a server command source that reports to it.

## Build and tests

On this workspace, run Gradle through `heavy`:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests
```

The root build produces Fabric and NeoForge JARs in `build/libs` and `common/build/libs/brainagelib-gametest-<version>.jar`; CI invokes `runAllGameTests` to run the Fabric production GameTests and the NeoForge GameTests on a NeoForge server against the release JAR. BrainageLib's own GameTests compile the GameTest support sources directly.

Builds and CI use MinecraftModConventions 3.1.2.
