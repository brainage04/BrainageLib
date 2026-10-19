# BrainageLib

BrainageLib is a server-side Fabric and NeoForge library for Minecraft 26.2. It provides consistent command feedback and one combined first-join help surface for brainage04's server mods. Vanilla clients do not install it.

On a player's first join to a world, BrainageLib sends one combined notice naming every registered server mod. The acknowledgement is stored by player UUID in `brainagelib-first-join-help.json` in that world's folder, so it persists across restarts and remains independent between worlds.

## Features

- Messages receive a grey mod-name prefix and a consistent yellow, green, or red body.
- Combined player help and operator configuration command lists for registered mods.
- One first-join notice instead of a separate message from each mod.

## Getting started

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.88 or newer
- Java 25 or newer
- Install BrainageLib on the server alongside the mods that require it.

## Commands

- `/servermods` or `/servermods help` - lists registered mods, their descriptions, and player help commands.
- `/servermods config` - lists operator configuration commands; requires operator permission level 2.

## Configuration

No configuration yet.

## Compatibility

Vanilla clients do not install it. Use the Fabric or NeoForge JAR for your server's loader.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - dependency setup, command feedback API, help registration, and development.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
