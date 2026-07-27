# BrainageLib

BrainageLib is a server-side Fabric and NeoForge library for Minecraft 26.2. It provides consistent command feedback and one combined first-join help surface for brainage04's server mods. Vanilla clients do not install it.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.23-beta or newer
- Java 25 or newer

## Dependency

BrainageLib 1.0.0 is published to Maven Central as `io.github.brainage04:brainagelib:1.0.0` for Fabric and `io.github.brainage04:brainagelib-neoforge:1.0.0` for NeoForge. GitHub Release artifacts are an artifact-only fallback.

```groovy
repositories {
    mavenCentral()
    ivy {
        name = "BrainageLibGitHubReleases"
        url = uri("https://github.com/brainage04/BrainageLib/releases/download")
        patternLayout {
            artifact "v[revision]/[artifact]-[revision].[ext]"
        }
        metadataSources {
            artifact()
        }
        content {
            includeModule "io.github.brainage04", "brainagelib"
            includeModule "io.github.brainage04", "brainagelib-neoforge"
        }
    }
}

dependencies {
    implementation "io.github.brainage04:brainagelib:1.0.0"
    productionRuntimeMods "io.github.brainage04:brainagelib:1.0.0"
}
```

For NeoForge, use the loader-specific artifact:

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib-neoforge:1.0.0"
}
```

Declare the runtime requirement in `fabric.mod.json`:

```json
{
  "depends": {
    "brainagelib": ">=1.0.0"
  }
}
```

For NeoForge, declare `brainagelib` as a required dependency in `META-INF/neoforge.mods.toml`.

For local workspace development, the sibling repository can be preferred through FabricModdingConventions' `workspaceDependencies` component.

## Migrating from the Fabric-only release

Install exactly one BrainageLib JAR matching the server loader: `brainagelib-<version>.jar` for Fabric or `brainagelib-neoforge-<version>.jar` for NeoForge. Remove the old JAR before switching loaders; do not install both artifacts together.

The mod ID remains `brainagelib`, and its first-join acknowledgement continues to use the world's `brainagelib-first-join-help.json` data file, so existing world state is retained. BrainageLib remains server-side only: install it on the server, not vanilla clients. Dependent Fabric mods require the Fabric artifact and `fabric-api`; dependent NeoForge mods require the NeoForge artifact and declare the same `brainagelib` mod ID in `META-INF/neoforge.mods.toml`.

The root `./gradlew build` produces both loader-specific artifacts under `build/libs`.

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

BrainageLib registers:

- `/servermods help` — lists registered mods, their descriptions, and player help commands.
- `/servermods config` — lists operator configuration commands.

On a player's first join to a world, BrainageLib sends one combined notice naming every registered server mod. The acknowledgement is stored in that world's data directory by player UUID, so it persists across restarts and remains independent between worlds.

## Development

```shell
./gradlew build
```

The root build produces Fabric and NeoForge JARs in `build/libs`; CI invokes `runAllProductionGameTests` to run Fabric production GameTests and the NeoForge GameTest server.

## Publishing

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
