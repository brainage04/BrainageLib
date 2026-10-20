package io.github.brainage04.brainagelib.help;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.storage.JsonFile;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.util.*;

/**
 * Which players have seen the combined first-join notice, per world. A file that fails to load is never
 * overwritten (see {@link JsonFile}); notices then go out once per session.
 */
public final class FirstJoinHelpState {
    private static final int SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "brainagelib-first-join-help.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<UUID> NOTIFIED_PLAYERS = new HashSet<>();
    private static JsonFile<SavedState> file;
    private FirstJoinHelpState() {}
    public static void serverStarted(MinecraftServer server) { load(server); }
    public static void serverStopping(MinecraftServer server) { save(server); }
    public static void serverStopped() { clear(); }
    public static void playerJoined(ServerPlayer player, MinecraftServer server) { onPlayerJoin(player, server); }
    private static synchronized void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        if (ServerModHelpRegistry.entries().isEmpty() || !NOTIFIED_PLAYERS.add(player.getUUID())) return;
        boolean operator = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
        player.sendSystemMessage(ServerModHelpRegistry.firstJoinMessage(operator));
        save(server);
    }
    private static synchronized void load(MinecraftServer server) {
        clear();
        file = stateFile(server);
        if (!(file.load() instanceof JsonFile.LoadResult.Loaded<SavedState>(SavedState state)) || state.notifiedPlayers == null) return;
        for (String value : state.notifiedPlayers) {
            if (value == null) { BrainageLib.LOGGER.warn("Ignoring null player UUID in {}", file.path()); continue; }
            try { NOTIFIED_PLAYERS.add(UUID.fromString(value)); }
            catch (IllegalArgumentException ignored) { BrainageLib.LOGGER.warn("Ignoring invalid player UUID in {}: {}", file.path(), value); }
        }
    }
    private static synchronized void save(MinecraftServer server) {
        if (file == null) file = stateFile(server);
        List<String> playerIds = NOTIFIED_PLAYERS.stream().map(UUID::toString).sorted(Comparator.naturalOrder()).toList();
        file.save(new SavedState(SCHEMA_VERSION, new ArrayList<>(playerIds)));
    }
    private static synchronized void clear() { NOTIFIED_PLAYERS.clear(); file = null; }
    private static JsonFile<SavedState> stateFile(MinecraftServer server) {
        return JsonFile.builder(server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME), GSON, SavedState.class)
                .decoder(document -> {
                    SavedState state = GSON.fromJson(document, SavedState.class);
                    if (state.schemaVersion != SCHEMA_VERSION) throw new JsonParseException("Unsupported schemaVersion " + state.schemaVersion);
                    return state;
                })
                .description("first-join help state")
                .build();
    }
    private record SavedState(int schemaVersion, List<String> notifiedPlayers) {}
}
