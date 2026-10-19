# Development

## Dependency

BrainageLib is not on Maven Central yet. Consumers build it from a sibling checkout; the coordinates are `io.github.brainage04:brainagelib:1.0.1` for Fabric and `io.github.brainage04:brainagelib-neoforge:1.0.1` for NeoForge.

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib:1.0.1"
    productionRuntimeMods "io.github.brainage04:brainagelib:1.0.1"
}
```

For NeoForge, use the loader-specific artifact:

```groovy
dependencies {
    implementation "io.github.brainage04:brainagelib-neoforge:1.0.1"
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

For workspace development, FabricModdingConventions' `workspaceDependencies` component resolves BrainageLib from a sibling checkout's `build/local-repo` (written by `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`):

```groovy
workspaceDependencies {
    siblingMaven('BrainageLib') {
        coordinate.set("io.github.brainage04:brainagelib:1.0.1")
        siblingDirectory.set(rootProject.layout.projectDirectory.dir('../BrainageLib'))
    }
}
```

In CI, pass `prepare_siblings: BrainageLib` to the FabricModdingConventions reusable workflows; they clone this repository into `../BrainageLib` and publish it to that local repository before building.

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

## Build and tests

On this workspace, run Gradle through `heavy`:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests -Pfabricmoddingconventions.devClientOptions=/nonexistent
```

The root build produces Fabric and NeoForge JARs in `build/libs`; CI invokes `runAllGameTests` to run the Fabric production GameTests and the NeoForge GameTests on a NeoForge server against the release JAR.

Builds and CI use FabricModdingConventions 2.4.26.
