package io.github.brainage04.brainagelib.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.brainage04.brainagelib.blocks.ConnectedBlocks.Adjacency;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class ConnectedBlocksTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    @Test
    void adjacencyDecidesWhichNeighboursConnect() {
        BlockPos face = ORIGIN.east();
        BlockPos edge = face.offset(1, 1, 0);
        BlockPos corner = edge.offset(1, 1, 1);
        Set<BlockPos> world = Set.of(face, edge, corner);
        assertEquals(List.of(face), find(world, 10, Adjacency.FACES));
        assertEquals(List.of(face, edge), find(world, 10, Adjacency.FACES_EDGES));
        assertEquals(List.of(face, edge, corner), find(world, 10, Adjacency.FACES_EDGES_CORNERS));
    }

    @Test
    void nearestComeFirstInAFixedOrderAndTheLimitCutsTheFarEnd() {
        Set<BlockPos> world = new HashSet<>();
        for (int x = 1; x <= 5; x++) world.add(ORIGIN.east(x));
        world.add(ORIGIN.west());
        world.add(ORIGIN.below());
        assertEquals(
                List.of(ORIGIN.west(), ORIGIN.below(), ORIGIN.east(), ORIGIN.east(2)),
                find(world, 4, Adjacency.FACES_EDGES_CORNERS));
        assertEquals(List.of(), find(world, 0, Adjacency.FACES_EDGES_CORNERS));
    }

    @Test
    void eachPositionIsAskedOnceAndTheOriginNever() {
        Set<BlockPos> world = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(-2, 62, -2, 2, 66, 2))
            world.add(pos.immutable());
        List<BlockPos> asked = new ArrayList<>();
        List<BlockPos> found =
                ConnectedBlocks.find(
                        ORIGIN,
                        1000,
                        Adjacency.FACES_EDGES_CORNERS,
                        pos -> {
                            asked.add(pos.immutable());
                            return world.contains(pos);
                        });
        assertEquals(124, found.size(), "every block of the 5x5x5 cube but the origin");
        assertEquals(new HashSet<>(asked).size(), asked.size(), "a position was asked twice");
        assertFalse(asked.contains(ORIGIN), "the origin was asked");
    }

    private static List<BlockPos> find(Set<BlockPos> world, int limit, Adjacency adjacency) {
        return ConnectedBlocks.find(ORIGIN, limit, adjacency, world::contains);
    }
}
