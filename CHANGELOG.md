# Changelog

## 2.2.0 (unreleased)

Nothing changes for players: the new API is for the mods that use it.

### New API

- `OwnerAccessList<K>`: which guests an owner lets use something of theirs (a spawn, a named home), with `grant` and `revoke` returning how many guests changed, `allows` answering whether a player may use it, `guests` giving a key's guests, and a callback that runs only when something changed. The mod's own codec saves it through `snapshot()` and `load(...)`.
- `CurrentServer.get()`: the server running in this game instance, or `null`, for code the game gives no server or level to. A server is current from before it loads its levels until after its stopped listeners. Use it from server code only: in a client it is the singleplayer server while one runs and `null` on a remote server. On NeoForge that is the dedicated, integrated and GameTest servers only.

### Build and publishing

- A mod that calls `OwnerAccessList` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.
- A mod that calls `CurrentServer` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.

## 2.1.0

BrainageLib 2.1.0 adds one API and changes nothing players see.

### New API

- `ConnectedBlocks` (`io.github.brainage04.brainagelib.blocks`): the breadth-first search a vein miner or tree feller runs from a broken block. `find(origin, limit, adjacency, member)` returns up to `limit` connected positions, nearest first, `origin` left out, visiting neighbours in one fixed order and asking `member` about each position once. `Adjacency` picks face (6), face and edge (18) or all 26 neighbours. `loaded(level, statePredicate)` reads only chunks already loaded and rejects positions outside the build height, so a walk never loads a chunk.

### Build and publishing

- A mod that calls `ConnectedBlocks` needs `">=2.1.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.1.0,3.0.0)"` in `neoforge.mods.toml`.

## 2.0.0

BrainageLib 2.0.0 is a major release: it removes public API, so mods built against 1.x may need changes. Telekinesis 1.2.0 uses none of the removed members and keeps working.

### What players see without updating their other mods

Every mod that hands drops to the player through BrainageLib (Telekinesis, and BrainageMinigames during a match) now covers more than blocks, mobs, explosions and leaves:

- An item frame, glow item frame, painting, cushion or armour stand a player breaks now goes to that player: the decoration's own item and the item it held, or the stand and its equipment. A blast the player caused counts too.
- A boat, raft or minecart a player breaks now goes to that player, with a chest boat's, chest minecart's or hopper minecart's contents.
- A mob's loot and experience go to the player the game credits with the kill (`LivingEntity#getKillCredit()`), so a kill the game credits to a player without a recorded player hit now counts too. A kill the game credits to a mob stays in the world.
- In a BrainageMinigames match, this applies to every player the Mined Drops setting or the Telekinesis perk covers.

A blast that names no player (a creeper's), a fire, lava, a mob, a decoration that lost its support and a creative break still leave the drops in the world.

### Removed API

- `BrainageLib.initialize()`
- `ConfirmationWindow.windowTicks()` and `ConfirmationWindow.isPending(key, tick)`
- `WorldPlayerStore.isOpen()`
- `JsonFile.savesAllowed()` (now private)
- `ModFeedback.decorate(...)` (now package-private)
- `AtomicFiles.write`, `writeText`, `replace`, `stagingPath` and `AtomicFiles.IOConsumer` (now package-private). `AtomicFiles.writeString` stays public.

### New API

- `ConfigFile<T>`: a mod's own JSON config with a default value, `loadOrCreate()`, `reload()` and `save()`. `save()` writes nothing before the first load or after a failed one, changing the value never changes the default, and saved files end with a line break.
- `HelpPage`: a mod's `/<mod>` and `/<mod> help` page in the shared feedback style.
- `ModFeedback.requirePlayer(source)` and `ModFeedback.PLAYERS_ONLY_MESSAGE`: bind the player who ran a command, or tell any other source that the command is for players.
- `GameTestPlayer.mock(helper)` and `mock(helper, gameMode)` in `brainagelib-gametest`: the supported replacement for `GameTestHelper.makeMockServerPlayerInLevel()`.
- `GameTestPlayer.join(level, profile)` in `brainagelib-gametest`: joins a player in a given level without a `GameTestHelper`, for example a second player in a client GameTest.

### Build and publishing

- MinecraftModConventions 3.1.2.
- Maven Central gets `brainagelib` (Fabric) and `brainagelib-neoforge` in one deployment. The test-only `brainagelib-gametest` is published only to the local repository for sibling builds.
- Consumers should cap the dependency below the next major version: `">=2.0.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.0.0,3.0.0)"` in `neoforge.mods.toml`.
