package io.github.brainage04.brainagelib.help;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.brainage04.brainagelib.BrainageLib;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class FirstJoinHelpState {
    private static final int SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "brainagelib-first-join-help.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<UUID> NOTIFIED_PLAYERS = new HashSet<>();
    private static Path filePath;
    /** Set when the world's file could not be read; it is then never overwritten and notices last for the session. */
    private static boolean loadFailed;
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
        clear(); filePath = statePath(server);
        if (!Files.isRegularFile(filePath)) return;
        try (Reader reader = Files.newBufferedReader(filePath)) {
            SavedState state = GSON.fromJson(reader, SavedState.class);
            if (state == null) throw new JsonParseException("First-join help state document is empty");
            if (state.schemaVersion != SCHEMA_VERSION) throw new JsonParseException("Unsupported first-join help state schemaVersion " + state.schemaVersion);
            if (state.notifiedPlayers == null) return;
            for (String value : state.notifiedPlayers) {
                if (value == null) { BrainageLib.LOGGER.warn("Ignoring null player UUID in {}", filePath); continue; }
                try { NOTIFIED_PLAYERS.add(UUID.fromString(value)); }
                catch (IllegalArgumentException ignored) { BrainageLib.LOGGER.warn("Ignoring invalid player UUID in {}: {}", filePath, value); }
            }
        } catch (IOException | JsonParseException exception) {
            NOTIFIED_PLAYERS.clear();
            loadFailed = true;
            BrainageLib.LOGGER.error("Failed to load first-join help state from {}; it will not be overwritten this session", filePath, exception);
        }
    }
    private static synchronized void save(MinecraftServer server) {
        if (filePath == null) filePath = statePath(server);
        if (loadFailed) {
            BrainageLib.LOGGER.warn("Not saving first-join help state to {} because it failed to load; the file is kept unchanged", filePath);
            return;
        }
        List<String> playerIds = NOTIFIED_PLAYERS.stream().map(UUID::toString).sorted(Comparator.naturalOrder()).toList();
        SavedState state = new SavedState(SCHEMA_VERSION, new ArrayList<>(playerIds));
        Path temporaryFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryFile)) { GSON.toJson(state, writer); }
            try { Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) { BrainageLib.LOGGER.error("Failed to save first-join help state to {}", filePath, exception); }
    }
    private static synchronized void clear() { NOTIFIED_PLAYERS.clear(); filePath = null; loadFailed = false; }
    private static Path statePath(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME); }
    private record SavedState(int schemaVersion, List<String> notifiedPlayers) {}
}
