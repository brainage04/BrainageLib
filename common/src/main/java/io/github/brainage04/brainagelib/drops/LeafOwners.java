package io.github.brainage04.brainagelib.drops;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Which player felled the tree each leaf belonged to, for the leaves that decay after it. A
 * decaying leaf drops through the game's entity-less loot path, with no breaker to name; a mod that
 * fells a tree calls {@link #felled} with the logs it broke, and every leaf those logs held up then
 * drops inside a capture scope for that player ({@link DropCapture}) when it decays, so each pays
 * exactly the player who cut its tree and never another feller whose log happens to stand nearer.
 *
 * <p>The reach is vanilla's own: a leaf a log held up is at most six leaf steps from it ({@link
 * LeavesBlock#DECAY_DISTANCE} one), found by walking face-adjacent leaves in loaded chunks only, so
 * no chunk is loaded for a felling. The one-minute window covers the decay a felling starts, so a
 * leaf that goes much later is decay the felling no longer owns. A tree nobody felled has no
 * record, so its leaves drop where they always did. Every feller is recorded; the {@link
 * DropPolicies} decide whether their leaves' drops reach an inventory or the ground.
 */
public final class LeafOwners {
    /** A leaf this many leaf steps from a felled log is the most one log can hold up. */
    private static final int REACH = LeavesBlock.DECAY_DISTANCE - 1;

    /** How long after a felling the leaves it left behind still count as the feller's. */
    private static final int WINDOW = 1_200;

    /** Leaves remembered per dimension at most; another felling first forgets the expired ones. */
    private static final int LIMIT = 65_536;

    private record Fall(UUID feller, long expires) {}

    /**
     * By dimension, not by level: a regenerated world has a new {@link ServerLevel}, and must not
     * keep the old one's fellings for the 65 thousand entries its dimension still allows.
     */
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Fall>> FELLINGS =
            new HashMap<>();

    private LeafOwners() {}

    /**
     * {@code player} felled a tree whose logs are at {@code logs}: every leaf those logs held up is
     * theirs when it decays later, whoever else's logs stand near it.
     */
    public static void felled(ServerPlayer player, ServerLevel level, List<BlockPos> logs) {
        if (logs.isEmpty()) {
            return;
        }
        long expires = level.getGameTime() + WINDOW;
        UUID feller = player.getUUID();
        Long2ObjectOpenHashMap<Fall> leaves =
                FELLINGS.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>());
        Fall fall = new Fall(feller, expires);
        for (long leaf : leavesOf(level, logs)) {
            leaves.put(leaf, fall);
        }
        if (leaves.size() > LIMIT) {
            prune(leaves, level.getGameTime());
        }
    }

    /**
     * The player who felled the tree the leaf at {@code pos} belonged to, if a felling in its
     * window recorded it, and they are still here.
     */
    public static @Nullable ServerPlayer owner(ServerLevel level, BlockPos pos) {
        Long2ObjectOpenHashMap<Fall> leaves = FELLINGS.get(level.dimension());
        if (leaves == null) {
            return null;
        }
        Fall fall = leaves.get(pos.asLong());
        if (fall == null) {
            return null;
        }
        if (fall.expires() < level.getGameTime()) {
            leaves.remove(pos.asLong());
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(fall.feller());
    }

    /**
     * The leaves the felled {@code logs} held up, as positions: face-adjacent leaves within {@link
     * #REACH} steps of one of them, from loaded chunks only.
     */
    private static LongList leavesOf(ServerLevel level, List<BlockPos> logs) {
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayList found = new LongArrayList();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos next = log.relative(direction);
                if (found.size() >= LIMIT) {
                    return found;
                }
                if (leaf(level, next) && seen.add(next.asLong())) {
                    found.add(next.asLong());
                    queue.addLast(next.immutable());
                }
            }
        }
        for (int step = 1; step < REACH && !queue.isEmpty(); step++) {
            for (int left = queue.size(); left > 0; left--) {
                BlockPos current = queue.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!leaf(level, next) || !seen.add(next.asLong())) {
                        continue;
                    }
                    if (found.size() >= LIMIT) {
                        return found;
                    }
                    found.add(next.asLong());
                    queue.addLast(next.immutable());
                }
            }
        }
        return found;
    }

    /** Whether a loaded leaf stands at {@code pos}. */
    private static boolean leaf(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof LeavesBlock;
    }

    /** Forgets every leaf record that has left the window. */
    private static void prune(Long2ObjectOpenHashMap<Fall> leaves, long now) {
        LongIterator keys = leaves.keySet().iterator();
        while (keys.hasNext()) {
            if (leaves.get(keys.nextLong()).expires() < now) {
                keys.remove();
            }
        }
    }
}
