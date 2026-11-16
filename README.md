# BrainageLib

BrainageLib is a server-side Fabric and NeoForge library for Minecraft 26.3. It provides shared code for a lot of my mods, including a drop capture engine, connected block searching, and much more!

## Features

- A drop capture engine (for inserting block and entity drops into the player inventory instead of dropping them on the ground - used in both Telekinesis and SkyportMinigames)
- Connected block searching (for detecting veins of connectd blocks - used in both VeinMiner and SkyportMinigames)
- Consistent command feedback - messages receive a grey mod-name prefix and a consistent coloured body (typically red for error messages, green for success messages, and yellow for warning or neutral messages)
- Combining help messages for each mod that uses this library into one message instead of clogging up the chat with a separate message from each mod
- Combining player help and operator config command lists for registered mods

## Dependencies

- Minecraft 26.3
- Fabric Loader >=0.19.5 w/ Fabric API, or NeoForge >=26.3.0.99
- Java >=25

## Commands

- `/servermods` or `/servermods help` - lists registered mods, descriptions, and player help commands
- `/servermods config` - lists operator configuration commands (requires operator permission level 2)

## Compatibility

Note: The drop capture for Telekinesis <1.2.0 uses the same game hooks as BrainageLib 2.0.0 and as a result refuses to load beside it (Fabric reports it as `breaks`, NeoForge reports it as `incompatible`). Update Telekinesis and BrainageLib at the same time to avoid this issue.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - dependency setup, command feedback, help registration, saved files, confirmation windows, drop capture, GameTest support, and development.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
