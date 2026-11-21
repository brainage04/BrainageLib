package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.drops.DropPolicies;
import io.github.brainage04.brainagelib.drops.DropPolicy;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ScaffoldingBlock;

/// The blocks that break a tick after their support goes: a player's break of the bottom block pays
/// them for the whole column, and a support break no player caused leaves every drop in the world.
///
/// <p>Sugar cane, cactus, bamboo, kelp, twisting and weeping vines, chorus plants, big dripleaf stems
/// and scaffolding do not break with their support. Their `updateShape` schedules a tick and their
/// `tick` then calls `level.destroyBlock(pos, true)` with no breaker, so the drops are outside every
/// capture scope: `SugarCaneBlock.java:37-38,74-75`, `CactusBlock.java:41-42,98-99`,
/// `BambooStalkBlock.java:100-101,138-139`, `GrowingPlantBodyBlock.java:42-43` with
/// `GrowingPlantBlock.java:55-56`, `ChorusPlantBlock.java:64-65,77-78`,
/// `BigDripleafStemBlock.java:79-80,92-93`, `ScaffoldingBlock.java:103,115-117`.
///
/// <p>Each test breaks the bottom block of a column and waits for the top to go, so the assertion
/// runs after every block of the column has broken. The player stands four blocks away, out of item
/// pickup range, so a drop that stays in the world is still there when the assertion runs.
public final class SupportBreakGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos COLUMN = new BlockPos(5, 1, 5);
    private static final BlockPos SECOND_COLUMN = new BlockPos(2, 1, 5);

    private SupportBreakGameTests() {}

    public static void sugarCaneColumnPaysTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.west(), Blocks.WATER.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.SUGAR_CANE.defaultBlockState());
        }
        runColumn(
                helper,
                "SbCane",
                sand.above(),
                sand.above(3),
                Blocks.SUGAR_CANE,
                (context, player) -> paysAll(context, player, Items.SUGAR_CANE, 3));
    }

    public static void cactusColumnPaysTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.CACTUS.defaultBlockState());
        }
        runColumn(
                helper,
                "SbCactus",
                sand.above(),
                sand.above(3),
                Blocks.CACTUS,
                (context, player) -> paysAll(context, player, Items.CACTUS, 3));
    }

    public static void bambooColumnPaysTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.BAMBOO.defaultBlockState());
        }
        runColumn(
                helper,
                "SbBamboo",
                sand.above(),
                sand.above(3),
                Blocks.BAMBOO,
                (context, player) -> paysAll(context, player, Items.BAMBOO, 3));
    }

    public static void kelpColumnPaysTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(stone.above(), Blocks.KELP_PLANT.defaultBlockState());
        level.setBlockAndUpdate(stone.above(2), Blocks.KELP_PLANT.defaultBlockState());
        level.setBlockAndUpdate(stone.above(3), Blocks.KELP.defaultBlockState());
        runColumn(
                helper,
                "SbKelp",
                stone,
                stone.above(3),
                Blocks.KELP,
                (context, player) -> paysAll(context, player, Items.KELP, 3));
    }

    /// Twisting vines grow up and weeping vines grow down, so each column loses its support from the
    /// other end. Both loot tables pay out on only a third of rolls with an empty hand
    /// (`data/minecraft/loot_table/blocks/twisting_vines_plant.json`, `table_bonus` at fortune 0), so
    /// ten blocks make an all-empty run a 1.8% chance.
    public static void vineColumnsPayTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos twisting = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(twisting, Blocks.STONE.defaultBlockState());
        for (int height = 1; height <= 4; height++) {
            level.setBlockAndUpdate(
                    twisting.above(height), Blocks.TWISTING_VINES_PLANT.defaultBlockState());
        }
        level.setBlockAndUpdate(twisting.above(5), Blocks.TWISTING_VINES.defaultBlockState());

        BlockPos weeping = helper.absolutePos(SECOND_COLUMN);
        level.setBlockAndUpdate(weeping.above(5), Blocks.STONE.defaultBlockState());
        for (int height = 1; height <= 4; height++) {
            level.setBlockAndUpdate(
                    weeping.above(height), Blocks.WEEPING_VINES_PLANT.defaultBlockState());
        }
        level.setBlockAndUpdate(weeping, Blocks.WEEPING_VINES.defaultBlockState());

        GameTestPlayer fixture = player(helper, "SbVines");
        ServerPlayer player = fixture.player();
        TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
        DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
        helper.startSequence()
                .thenExecute(
                        () -> {
                            helper.assertTrue(
                                    player.gameMode.destroyBlock(twisting),
                                    "The twisting vines' support must break");
                            helper.assertTrue(
                                    player.gameMode.destroyBlock(weeping.above(5)),
                                    "The weeping vines' support must break");
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(twisting.above(5)).isAir()
                                                && level.getBlockState(weeping).isAir(),
                                        "Both vine columns must break"))
                .thenExecute(
                        () -> {
                            try {
                                int vines =
                                        player.getInventory().countItem(Items.TWISTING_VINES)
                                                + player.getInventory()
                                                        .countItem(Items.WEEPING_VINES);
                                helper.assertTrue(
                                        vines > 0,
                                        "The player must receive the vines of both columns, got "
                                                + vines);
                                int ground = groundItems(helper, null);
                                helper.assertTrue(
                                        ground == 0,
                                        "No drop of the vine columns may stay on the ground, got "
                                                + ground);
                            } finally {
                                DropPolicies.unregister(policy);
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// A chorus plant's loot rolls 0 or 1 fruit (`data/minecraft/loot_table/blocks/chorus_plant.json`,
    /// `set_count` uniform 0 to 1), so two columns of five make an all-empty run a 1 in 1024 chance.
    public static void chorusColumnsPayTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(COLUMN);
        BlockPos second = helper.absolutePos(SECOND_COLUMN);
        for (BlockPos base : new BlockPos[] {first, second}) {
            level.setBlockAndUpdate(base, Blocks.END_STONE.defaultBlockState());
            for (int height = 1; height <= 5; height++) {
                level.setBlockAndUpdate(
                        base.above(height), Blocks.CHORUS_PLANT.defaultBlockState());
            }
        }

        GameTestPlayer fixture = player(helper, "SbChorus");
        ServerPlayer player = fixture.player();
        TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
        DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
        helper.startSequence()
                .thenExecute(
                        () -> {
                            helper.assertTrue(
                                    player.gameMode.destroyBlock(first.above()),
                                    "The first chorus column must break");
                            helper.assertTrue(
                                    player.gameMode.destroyBlock(second.above()),
                                    "The second chorus column must break");
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(first.above(5)).isAir()
                                                && level.getBlockState(second.above(5)).isAir(),
                                        "Both chorus columns must break"))
                .thenExecute(
                        () -> {
                            try {
                                int fruit = player.getInventory().countItem(Items.CHORUS_FRUIT);
                                helper.assertTrue(
                                        fruit > 0,
                                        "The player must receive the chorus fruit of both columns,"
                                                + " got "
                                                + fruit);
                                int ground = groundItems(helper, null);
                                helper.assertTrue(
                                        ground == 0,
                                        "No drop of the chorus columns may stay on the ground, got "
                                                + ground);
                            } finally {
                                DropPolicies.unregister(policy);
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// The head goes with the stems: `BigDripleafBlock.updateShape` returns air for a head whose
    /// support is gone (`BigDripleafBlock.java:149-151`), and `Block.updateOrDestroy` breaks it with
    /// its drops (`Block.java:222-225`).
    public static void bigDripleafStemsPayTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos dirt = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(dirt, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(dirt.above(), Blocks.BIG_DRIPLEAF_STEM.defaultBlockState());
        level.setBlockAndUpdate(dirt.above(2), Blocks.BIG_DRIPLEAF_STEM.defaultBlockState());
        level.setBlockAndUpdate(dirt.above(3), Blocks.BIG_DRIPLEAF.defaultBlockState());
        runColumn(
                helper,
                "SbDripleaf",
                dirt,
                dirt.above(3),
                Blocks.BIG_DRIPLEAF,
                (context, player) -> paysAll(context, player, Items.BIG_DRIPLEAF, 3));
    }

    /// Scaffolding's `updateShape` schedules a tick on every neighbour change
    /// (`ScaffoldingBlock.java:101-104`), so the distances are set up front and the support is the
    /// stone the column stands on.
    public static void scaffoldingColumnPaysTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(
                    stone.above(height),
                    Blocks.SCAFFOLDING
                            .defaultBlockState()
                            .setValue(ScaffoldingBlock.DISTANCE, height - 1));
        }
        runColumn(
                helper,
                "SbScaffolding",
                stone,
                stone.above(3),
                Blocks.SCAFFOLDING,
                (context, player) -> paysAll(context, player, Items.SCAFFOLDING, 3));
    }

    /// The support removed by the game rather than by a player's break: the call a piston's pop and
    /// an explosion make. No break scope is open, so the column's drops stay in the world even
    /// though a policy would deliver to the player standing by.
    public static void supportBrokenWithoutAPlayerLeavesDropsInTheWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.west(), Blocks.WATER.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.SUGAR_CANE.defaultBlockState());
        }

        GameTestPlayer fixture = player(helper, "SbNoPlayer");
        ServerPlayer player = fixture.player();
        TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
        DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
        helper.startSequence()
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        level.destroyBlock(sand, true),
                                        "The support must break"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(sand.above(3)).isAir(),
                                        "The column above must break"))
                .thenExecute(
                        () -> {
                            try {
                                int paid = player.getInventory().countItem(Items.SUGAR_CANE);
                                helper.assertTrue(
                                        paid == 0,
                                        "A support break no player caused must not pay the player,"
                                                + " got "
                                                + paid);
                                int ground = groundItems(helper, Items.SUGAR_CANE);
                                helper.assertTrue(
                                        ground == 3,
                                        "The whole column must drop in the world, got " + ground);
                            } finally {
                                DropPolicies.unregister(policy);
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// Breaks {@code bottom} for the player, waits for {@code top} to stop being {@code column},
    /// then runs {@code check}. Kelp's place is water again once it breaks, so the wait cannot ask
    /// for air.
    private static void runColumn(
            GameTestHelper helper,
            String name,
            BlockPos bottom,
            BlockPos top,
            Block column,
            ColumnCheck check) {
        GameTestPlayer fixture = player(helper, name);
        ServerPlayer player = fixture.player();
        TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
        DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
        helper.startSequence()
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        player.gameMode.destroyBlock(bottom),
                                        "The bottom block must break"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        !helper.getLevel().getBlockState(top).is(column),
                                        "The column above must break"))
                .thenExecute(
                        () -> {
                            try {
                                check.run(helper, player);
                            } finally {
                                DropPolicies.unregister(policy);
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    private static void paysAll(
            GameTestHelper helper, ServerPlayer player, Item item, int expected) {
        int got = player.getInventory().countItem(item);
        helper.assertTrue(
                got == expected,
                "The player must receive all " + expected + " " + item + ", got " + got);
        int ground = groundItems(helper, null);
        helper.assertTrue(
                ground == 0, "No drop of the column may stay on the ground, got " + ground);
    }

    private static GameTestPlayer player(GameTestHelper helper, String name) {
        GameTestPlayer fixture =
                GameTestPlayer.join(helper, name)
                        .markLoaded()
                        .gameMode(GameType.SURVIVAL)
                        .moveTo(helper, PLAYER_POS);
        fixture.player().getInventory().clearContent();
        return fixture;
    }

    /// Items inside the test's own bounds only: the server lays tests out 8 to a row, five blocks
    /// apart (`GameTestServer.java:325`, `StructureGridSpawner.java:17-18`), so a wider box could
    /// count a neighbour's drops.
    private static int groundItems(GameTestHelper helper, Item item) {
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, helper.getBounds())
                .stream()
                .filter(entity -> item == null || entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private interface ColumnCheck {
        void run(GameTestHelper helper, ServerPlayer player);
    }

    private record TestPolicy(Set<UUID> players, boolean deliver) implements DropPolicy {
        @Override
        public boolean governs(ServerPlayer player) {
            return players.contains(player.getUUID());
        }

        @Override
        public boolean delivers(ServerPlayer player) {
            return deliver;
        }

        @Override
        public boolean accepts(ServerPlayer player, ItemStack stack) {
            return true;
        }
    }
}
