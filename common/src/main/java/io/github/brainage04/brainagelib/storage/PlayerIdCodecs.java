package io.github.brainage04.brainagelib.storage;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Codecs for saved data keyed by players' UUIDs, written as UUID text ({@code
 * "0f8fad5b-d9cb-469f-a165-70867728950e"}) through the game's own {@link UUIDUtil#STRING_CODEC}.
 *
 * <p>A UUID that does not parse is a codec error for that entry only: the game's saved-data loader
 * keeps the partial result, so the other players' entries load and the damaged one is dropped. A
 * codec that throws from {@code UUID.fromString} instead makes the loader discard the whole file
 * and start the saved data empty.
 */
public final class PlayerIdCodecs {
    /**
     * A set of players, written as a list of UUID text in the set's order. Reading keeps the list's
     * order, drops repeats and returns a mutable set.
     */
    public static final Codec<Set<UUID>> PLAYER_SET =
            UUIDUtil.STRING_CODEC.listOf().<Set<UUID>>xmap(LinkedHashSet::new, List::copyOf);

    private PlayerIdCodecs() {}

    /** A map from each player's UUID, written as an object keyed by UUID text, to a value. */
    public static <V> Codec<Map<UUID, V>> byPlayer(Codec<V> valueCodec) {
        return Codec.unboundedMap(UUIDUtil.STRING_CODEC, valueCodec);
    }
}
