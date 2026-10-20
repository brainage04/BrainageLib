# Development

## Dependency

BrainageLib is not on Maven Central yet. Consumers build it from a sibling checkout; the coordinates are `io.github.brainage04:brainagelib:1.1.0` for Fabric and `io.github.brainage04:brainagelib-neoforge:1.1.0` for NeoForge.

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib:1.1.0"
    productionRuntimeMods "io.github.brainage04:brainagelib:1.1.0"
}
```

For NeoForge, use the loader-specific artifact:

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib-neoforge:1.1.0"
}
```

Declare the runtime requirement in `fabric.mod.json`:

```json
{
  "depends": {
    "brainagelib": ">=1.1.0"
  }
}
```

For NeoForge, declare `brainagelib` as a required dependency in `META-INF/neoforge.mods.toml`.

For workspace development, MinecraftModConventions' `workspaceDependencies` component resolves BrainageLib from a sibling checkout's `build/local-repo` (written by `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`):

```groovy
workspaceDependencies {
    siblingMaven('BrainageLib') {
        coordinate.set("io.github.brainage04:brainagelib:1.1.0")
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

## GameTest support

`io.github.brainage04:brainagelib-gametest` is a test-only artifact; never ship it or depend on it from main code. It is a small mod on both loaders, so GameTest runs load it next to the test mod:

```groovy
workspaceDependencies {
    siblingMaven('BrainageLibGameTest') {
        coordinate.set("io.github.brainage04:brainagelib-gametest:1.1.0")
        siblingDirectory.set(rootProject.layout.projectDirectory.dir('../BrainageLib'))
    }
}

dependencies {
    gametestImplementation "io.github.brainage04:brainagelib-gametest:1.1.0"
    productionRuntimeMods "io.github.brainage04:brainagelib-gametest:1.1.0"
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

Builds and CI use MinecraftModConventions 3.0.1.
