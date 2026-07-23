# BrainageLib

BrainageLib is a server-side Fabric library for Minecraft 26.2. It provides consistent command feedback and one combined first-join help surface for brainage04's server mods. Vanilla clients do not install it.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Java 25 or newer

## Dependency

BrainageLib 1.0.0 uses Maven coordinates `io.github.brainage04:brainagelib:1.0.0`. Maven Central is preferred; the public GitHub Release artifact is an artifact-only fallback.

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
        }
    }
}

dependencies {
    implementation "io.github.brainage04:brainagelib:1.0.0"
    productionRuntimeMods "io.github.brainage04:brainagelib:1.0.0"
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

For local workspace development, the sibling repository can be preferred through FabricModdingConventions' `workspaceDependencies` component.

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
./gradlew test runGameTest
```

`test` verifies Fabric metadata and API contracts. `runGameTest` verifies command registration and combined help behavior on a dedicated GameTest server.

## Publishing

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
