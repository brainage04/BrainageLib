package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.blocks.ConnectedBlocks;
import io.github.brainage04.brainagelib.blocks.ConnectedBlocks.Adjacency;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/// The connected-block walk in a real world, as a vein miner uses it.
public final class ConnectedBlocksGameTests {
    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);

    /// A million blocks away: no test or player keeps a chunk loaded there.
    private static final int FAR = 1_000_000;

    private ConnectedBlocksGameTests() {}

    /// The walk over loaded chunks finds the connected blocks its member accepts, and a position
    /// above the build height or in a chunk that is not loaded is no member: nothing is loaded.
    public static void loadedWalkReadsOnlyLoadedChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(ORIGIN);
        level.setBlockAndUpdate(origin, Blocks.EMERALD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin.east(), Blocks.EMERALD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin.east(2), Blocks.EMERALD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin.east(3), Blocks.DIAMOND_BLOCK.defaultBlockState());

        List<BlockPos> found =
                ConnectedBlocks.find(
                        origin,
                        10,
                        Adjacency.FACES,
                        ConnectedBlocks.loaded(level, state -> state.is(Blocks.EMERALD_BLOCK)));
        check(
                found.equals(List.of(origin.east(), origin.east(2))),
                "The walk must find the two connected emerald blocks: " + found);

        Predicate<BlockPos> anything = ConnectedBlocks.loaded(level, state -> true);
        check(anything.test(origin), "A loaded block must be read");
        check(
                !anything.test(origin.atY(level.getMaxY() + 1)),
                "A position above the build height must be no member");
        BlockPos far = origin.offset(FAR, 0, FAR);
        int chunkX = far.getX() >> 4;
        int chunkZ = far.getZ() >> 4;
        check(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "The far chunk was loaded before the walk");
        check(!anything.test(far), "A block in a chunk that is not loaded must be no member");
        check(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "Reading the far block loaded its chunk");
        helper.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
