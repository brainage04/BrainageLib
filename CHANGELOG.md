# Changelog

## 2.2.0 (unreleased)

BrainageLib 2.2.0 adds new API for the mods that use it, and one fix to drop capture that players see.

### Drop capture

- A player who breaks the bottom of a sugar cane, cactus, bamboo, kelp, twisting or weeping vine,
  chorus plant, big dripleaf or scaffolding column now gets the whole column. Those blocks break a
  tick after their support goes, through `level.destroyBlock(pos, true)` with no breaker, so their
  drops used to land on the ground. A support the game removes with no player behind it, such as a
  piston, an explosion or flowing water, still leaves those drops in the world, and so does a
  creative player's break.

### New API

- `OwnerAccessList<K>`: which guests an owner lets use something of theirs (a spawn, a named home), with `grant` and `revoke` returning how many guests changed, `allows` answering whether a player may use it, `guests` giving a key's guests, and a callback that runs only when something changed. The mod's own codec saves it through `snapshot()` and `load(...)`.
- `CurrentServer.get()`: the server running in this game instance, or `null`, for code the game gives no server or level to. A server is current from before it loads its levels until after its stopped listeners. Use it from server code only: in a client it is the singleplayer server while one runs and `null` on a remote server. On NeoForge that is the dedicated, integrated and GameTest servers only.
- `ServerPlatform.get()`: the config directory, command registration, server starting, started, stopping and stopped, and the end of every server tick, from common code with the same meaning on Fabric and NeoForge. Command registrars get the server's command selection, and stopping listeners run on every stop, also after a crash or a failed start. "Starting" is before the server loads its levels on both loaders, which on NeoForge is `ServerAboutToStartEvent`, not `ServerStartingEvent`.
- `GameRuleSet`: one mod's boolean and integer game rules with their ids, built in common code and registered once from each loader (`registerAll`). Rules built this way behave like the game's own under `/gamerule`, in the world's saved rules and in the edit-game-rules screen.
- `GameRuleFactory.bool(...)` and `GameRuleFactory.integer(...)` build the same rules without an id.
- `NameLookup` (`io.github.brainage04.brainagelib.lookup`): asks Mojang for the account behind a name the server has not cached, off the server thread, and hands the answer back on the server thread. `of(server, threadName, logger)` gives the server's one lookup, `lookup(name, callback)` starts one and answers on the server thread, except a name that cannot be a player name, which is answered at once on the calling thread; `install`/`reset` replace the server's lookup for a test. `Answer.status()` is `FOUND`, `NOT_FOUND`, `FAILED`, `TOO_MANY` or `STALE`; a `FOUND` answer older than 30 s arrives as `STALE`, so a command can cache the name and ask the player to run it again. The server's share of Mojang's limit is 100 requests per 2 minutes, charged on the lookup thread just before the request.
- `CommandRateLimit` (`io.github.brainage04.brainagelib.lookup`): how often one player may use a command, once a second, 20 a minute and 400 an hour. `of(server)` is the server's main allowance and `of(server, name)` a further one kept apart from it, for a command that also prints a candidate list; the name is shared by every mod on the server, so prefix it with the mod id. `refuse(player)` returns the wait in nanoseconds, or zero when the use is allowed and counted; `describe(nanos)` words it for a message.

### Build and publishing

- A mod that calls `OwnerAccessList` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.
- A mod that calls `CurrentServer` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.
- A mod that calls `ServerPlatform` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.
- A mod that calls `NameLookup` or `CommandRateLimit` needs `">=2.2.0 <3.0.0"` in `fabric.mod.json` and `versionRange="[2.2.0,3.0.0)"` in `neoforge.mods.toml`.

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
