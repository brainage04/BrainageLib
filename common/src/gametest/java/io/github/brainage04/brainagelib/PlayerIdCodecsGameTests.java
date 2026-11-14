package io.github.brainage04.brainagelib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.brainage04.brainagelib.storage.PlayerIdCodecs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Saved data keyed through {@link PlayerIdCodecs} loads through the game's own saved-data storage,
 * and one damaged player key costs only that player's entry.
 */
public final class PlayerIdCodecsGameTests {
    private static final UUID FIRST = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID SECOND = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "gametest_player_counts");

    private PlayerIdCodecsGameTests() {}

    public static void damagedPlayerKeyLosesOnlyThatPlayer(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Path directory = createTemporaryDirectory();
        try {
            writeSavedCounts(directory);

            PlayerCounts loaded = load(server, directory, PlayerCounts.TYPE);
            context.assertValueEqual(
                    loaded.counts,
                    Map.of(FIRST, 3, SECOND, 5),
                    "The undamaged players' entries load");

            PlayerCounts copied = load(server, directory, PlayerCounts.COPIED_TYPE);
            context.assertValueEqual(
                    copied.counts,
                    Map.of(),
                    "The mods' own UUID codec loses every player's entry to one damaged key");
            context.assertTrue(
                    copied.isDirty(), "The empty replacement is saved over the file next time");
        } finally {
            deleteRecursively(directory);
        }
        context.succeed();
    }

    // NeoForge 26.3.0.99 deprecates the game's constructor in favour of its own ServerLevel one,
    // which common code cannot call; the game's constructor is dec/server-26.3
    // SavedDataStorage.java:50.
    @SuppressWarnings("deprecation")
    private static PlayerCounts load(
            MinecraftServer server, Path directory, SavedDataType<PlayerCounts> type) {
        try (SavedDataStorage storage =
                new SavedDataStorage(directory, server.getFixerUpper(), server.registryAccess())) {
            PlayerCounts counts = storage.computeIfAbsent(type);
            boolean dirty = counts.isDirty();
            PlayerCounts snapshot = new PlayerCounts(counts.counts);
            snapshot.setDirty(dirty);
            // Closing would save the replacement over the file the second load reads.
            counts.setDirty(false);
            return snapshot;
        }
    }

    private static void writeSavedCounts(Path directory) {
        CompoundTag players = new CompoundTag();
        players.putInt(FIRST.toString(), 3);
        players.putInt("not-a-uuid", 4);
        players.putInt(SECOND.toString(), 5);
        CompoundTag data = new CompoundTag();
        data.put("players", players);
        CompoundTag file = new CompoundTag();
        file.put("data", data);
        NbtUtils.addCurrentDataVersion(file);
        Path path = ID.withSuffix(".dat").resolveAgainst(directory);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(file, path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path createTemporaryDirectory() {
        try {
            return Files.createTempDirectory("brainagelib-player-ids");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteRecursively(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static final class PlayerCounts extends SavedData {
        private static final Codec<PlayerCounts> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                PlayerIdCodecs.byPlayer(Codec.INT)
                                                        .optionalFieldOf("players", Map.of())
                                                        .forGetter(counts -> counts.counts))
                                        .apply(instance, PlayerCounts::new));
        private static final SavedDataType<PlayerCounts> TYPE =
                new SavedDataType<>(ID, PlayerCounts::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

        /** The codec SimpleHomes, SpawnCommands and BetterVillagerTrades write themselves. */
        private static final Codec<PlayerCounts> COPIED_CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                Codec.unboundedMap(
                                                                Codec.STRING.xmap(
                                                                        UUID::fromString,
                                                                        UUID::toString),
                                                                Codec.INT)
                                                        .optionalFieldOf("players", Map.of())
                                                        .forGetter(counts -> counts.counts))
                                        .apply(instance, PlayerCounts::new));

        private static final SavedDataType<PlayerCounts> COPIED_TYPE =
                new SavedDataType<>(
                        ID, PlayerCounts::new, COPIED_CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

        private final Map<UUID, Integer> counts;

        private PlayerCounts() {
            this(Map.of());
        }

        private PlayerCounts(Map<UUID, Integer> counts) {
            this.counts = new HashMap<>(counts);
        }
    }
}
