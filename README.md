# BrainageLib

BrainageLib is a server-side Fabric and NeoForge library for Minecraft 26.3. It provides consistent command feedback, one combined first-join help surface and a shared drop capture engine for brainage04's server mods. Vanilla clients do not install it.

On a player's first join to a world, BrainageLib sends one combined notice naming every registered server mod. The acknowledgement is stored by player UUID in `brainagelib-first-join-help.json` in that world's folder, so it persists across restarts and remains independent between worlds.

## Features

- Messages receive a grey mod-name prefix and a consistent yellow, green, or red body.
- Combined player help and operator configuration command lists for registered mods.
- One first-join notice instead of a separate message from each mod.
- Drop capture shared by Telekinesis and Skyport Minigames: drops and experience from blocks a player breaks, mobs the game credits them with killing, item frames, glow item frames, paintings, cushions and armour stands they break, boats and minecarts they break with their contents, explosions they cause (including beds and respawn anchors they use) and the decaying leaves of trees they fell go to that player, as the installed mods' rules decide. On its own BrainageLib delivers nothing.

## Getting started

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer with Fabric API, or NeoForge 26.3.0.99 or newer
- Java 25 or newer
- Install BrainageLib on the server alongside the mods that require it.

## Commands

- `/servermods` or `/servermods help` - lists registered mods, their descriptions, and player help commands.
- `/servermods config` - lists operator configuration commands; requires operator permission level 2.

## Configuration

No configuration yet.

## Compatibility

Vanilla clients do not install it. Use the Fabric or NeoForge JAR for your server's loader. A client that installs a mod requiring BrainageLib, such as Skyport Minigames, installs it too, so its Modrinth listing marks it required on the client as well.

Telekinesis before 1.2.0 carries its own drop capture on the same game hooks; BrainageLib 2.0.0 refuses to load beside it (Fabric `breaks`, NeoForge `incompatible`), so update Telekinesis together with BrainageLib.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - dependency setup, loader platform, command feedback, help registration, saved files, confirmation windows, game rules, drop capture, GameTest support, and development.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
