package io.github.brainage04.brainagelib.blocks;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * The blocks connected to one block, as a vein miner or a tree feller finds them: a breadth-first
 * walk from the block through its neighbours that {@code member} accepts, nearest first. The walk
 * visits the neighbours of each block in one fixed order (x, then y, then z offsets, each from -1
 * to 1), so the same world always gives the same blocks in the same order, and a limit cuts the
 * same far end off.
 */
public final class ConnectedBlocks {
    /** Which neighbours of a block count as connected to it. */
    public enum Adjacency {
        /** The six blocks sharing a face. */
        FACES,
        /** The six sharing a face and the twelve sharing an edge. */
        FACES_EDGES,
        /** All 26 blocks around it, corners included. */
        FACES_EDGES_CORNERS;

        /** Whether the block at this offset (not 0, 0, 0) is a neighbour. */
        public boolean includes(int dx, int dy, int dz) {
            int axes = (dx == 0 ? 0 : 1) + (dy == 0 ? 0 : 1) + (dz == 0 ? 0 : 1);
            return switch (this) {
                case FACES -> axes == 1;
                case FACES_EDGES -> axes <= 2;
                case FACES_EDGES_CORNERS -> true;
            };
        }
    }

    private ConnectedBlocks() {}

    /**
     * Up to {@code limit} positions connected to {@code origin} through positions {@code member}
     * accepts, nearest first, {@code origin} itself left out. {@code member} is asked about each
     * position at most once and reads the world itself, so it decides whether a chunk may be
     * loaded; {@link #loaded} reads loaded chunks only.
     */
    public static List<BlockPos> find(
            BlockPos origin, int limit, Adjacency adjacency, Predicate<BlockPos> member) {
        List<BlockPos> found = new ArrayList<>();
        if (limit <= 0) return found;
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin.asLong());
        queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0 || !adjacency.includes(x, y, z)) continue;
                        BlockPos next = current.offset(x, y, z);
                        if (!seen.add(next.asLong()) || !member.test(next)) continue;
                        found.add(next);
                        if (found.size() >= limit) return found;
                        queue.addLast(next);
                    }
                }
            }
        }
        return found;
    }

    /**
     * A {@link #find} member that accepts a position whose block {@code state} accepts, read only
     * from a chunk already loaded: a position in a chunk that is not, or outside the build height,
     * is no member, and no chunk is loaded for it. It remembers the last chunk it read, so use it
     * for one walk only. Call it on the server thread: getChunkNow returns null elsewhere
     * (ServerChunkCache.java:176-178), so the walk would find nothing.
     */
    public static Predicate<BlockPos> loaded(ServerLevel level, Predicate<BlockState> state) {
        return new Predicate<>() {
            private @Nullable LevelChunk chunk;

            @Override
            public boolean test(BlockPos pos) {
                if (level.isOutsideBuildHeight(pos)) return false;
                int chunkX = SectionPos.blockToSectionCoord(pos.getX());
                int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
                if (chunk == null || chunk.getPos().x() != chunkX || chunk.getPos().z() != chunkZ) {
                    chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) return false;
                }
                return state.test(chunk.getBlockState(pos));
            }
        };
    }
}
