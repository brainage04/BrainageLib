package io.github.brainage04.brainagelib.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class PlayerIdCodecsTest {
    private static final UUID FIRST = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID SECOND = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    /** The UUID codec SimpleHomes, SpawnCommands and BetterVillagerTrades each define. */
    private static final Codec<UUID> COPIED_UUID =
            Codec.STRING.xmap(UUID::fromString, UUID::toString);

    @Test
    void byPlayerWritesWhatTheModsCopiesWrite() {
        Map<UUID, Integer> players = new LinkedHashMap<>();
        players.put(FIRST, 3);
        players.put(SECOND, 5);

        Codec<Map<UUID, Integer>> codec = PlayerIdCodecs.byPlayer(Codec.INT);
        Tag written = codec.encodeStart(NbtOps.INSTANCE, players).getOrThrow();
        Tag copied =
                Codec.unboundedMap(COPIED_UUID, Codec.INT)
                        .encodeStart(NbtOps.INSTANCE, players)
                        .getOrThrow();

        assertEquals(copied, written);
        CompoundTag tag = assertInstanceOf(CompoundTag.class, written);
        assertEquals(Set.of(FIRST.toString(), SECOND.toString()), tag.keySet());
        assertEquals(players, codec.parse(NbtOps.INSTANCE, copied).getOrThrow());
    }

    @Test
    void damagedPlayerKeyDropsOnlyThatPlayer() {
        JsonObject saved = new JsonObject();
        saved.addProperty(FIRST.toString(), 3);
        saved.addProperty("not-a-uuid", 4);
        saved.addProperty(SECOND.toString(), 5);

        DataResult<Map<UUID, Integer>> result =
                PlayerIdCodecs.byPlayer(Codec.INT).parse(JsonOps.INSTANCE, saved);

        assertTrue(result.isError(), "The damaged key is reported");
        assertTrue(result.error().orElseThrow().message().contains("not-a-uuid"));
        assertEquals(
                Map.of(FIRST, 3, SECOND, 5),
                result.resultOrPartial().orElseThrow(),
                "The other players' entries are kept");
    }

    @Test
    void theModsCopiesThrowOnADamagedKey() {
        JsonObject saved = new JsonObject();
        saved.addProperty(FIRST.toString(), 3);
        saved.addProperty("not-a-uuid", 4);

        assertThrows(
                IllegalArgumentException.class,
                () -> Codec.unboundedMap(COPIED_UUID, Codec.INT).parse(JsonOps.INSTANCE, saved));
    }

    @Test
    void playerSetRoundTripsAsUuidTextInOrder() {
        Set<UUID> players = new LinkedHashSet<>(List.of(SECOND, FIRST));

        JsonElement written =
                PlayerIdCodecs.PLAYER_SET.encodeStart(JsonOps.INSTANCE, players).getOrThrow();

        JsonArray expected = new JsonArray();
        expected.add(new JsonPrimitive(SECOND.toString()));
        expected.add(new JsonPrimitive(FIRST.toString()));
        assertEquals(expected, written);
        Codec<List<UUID>> copiedList = COPIED_UUID.listOf();
        JsonElement copied =
                copiedList.encodeStart(JsonOps.INSTANCE, List.copyOf(players)).getOrThrow();
        assertEquals(copied, written, "The set is written the way the mods write their guest lists");
        Set<UUID> read = PlayerIdCodecs.PLAYER_SET.parse(JsonOps.INSTANCE, written).getOrThrow();
        assertEquals(List.of(SECOND, FIRST), List.copyOf(read));
    }

    @Test
    void playerSetDropsRepeatsAndIsMutable() {
        JsonArray saved = new JsonArray();
        saved.add(FIRST.toString());
        saved.add(FIRST.toString());

        Set<UUID> read = PlayerIdCodecs.PLAYER_SET.parse(JsonOps.INSTANCE, saved).getOrThrow();

        assertEquals(Set.of(FIRST), read);
        read.add(SECOND);
        assertEquals(Set.of(FIRST, SECOND), read);
    }

    @Test
    void damagedPlayerInASetDropsOnlyThatPlayer() {
        JsonArray saved = new JsonArray();
        saved.add(FIRST.toString());
        saved.add("not-a-uuid");
        saved.add(SECOND.toString());

        DataResult<Set<UUID>> result = PlayerIdCodecs.PLAYER_SET.parse(JsonOps.INSTANCE, saved);

        assertTrue(result.isError(), "The damaged entry is reported");
        assertEquals(Set.of(FIRST, SECOND), result.resultOrPartial().orElseThrow());
    }
}
