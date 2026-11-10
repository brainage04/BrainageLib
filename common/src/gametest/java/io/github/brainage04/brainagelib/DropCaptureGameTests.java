package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.drops.DropPolicies;
import io.github.brainage04.brainagelib.drops.DropPolicy;
import io.github.brainage04.brainagelib.drops.LeafOwners;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// Each policy governs only the players of its test, so tests running side by side never decide
/// for each other.
public final class DropCaptureGameTests {
    private static final BlockPos FIRST_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND_POS = new BlockPos(5, 1, 1);
    private static final BlockPos BLOCK_POS = new BlockPos(3, 1, 3);
    private static final BlockPos FAR_POS = new BlockPos(3, 1, 10);

    /// Rolls of a decaying oak leaf's loot table. It pays out on only about 7% of leaves (a sapling
    /// 5%, sticks 2%, an apple 0.5%; `data/minecraft/loot_table/blocks/oak_leaves.json`), so 300
    /// rolls all empty has probability about 1e-10.
    private static final int LEAF_ROLLS = 300;

    private DropCaptureGameTests() {}

    public static void highestGoverningPolicyDecides(GameTestHelper helper) {
        try (GameTestPlayer inMatch = player(helper, "DcInMatch", FIRST_POS);
                GameTestPlayer outside = player(helper, "DcOutside", SECOND_POS)) {
            TestPolicy general =
                    new TestPolicy(
                            Set.of(inMatch.player().getUUID(), outside.player().getUUID()), true);
            TestPolicy match = new TestPolicy(Set.of(inMatch.player().getUUID()), false);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, general);
            DropPolicies.register(DropPolicies.MATCH_PRIORITY, match);
            try {
                BlockPos matchBlock = helper.absolutePos(FIRST_POS.above(2));
                BlockPos outsideBlock = helper.absolutePos(SECOND_POS.above(2));
                helper.getLevel().setBlockAndUpdate(matchBlock, Blocks.DIRT.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(outsideBlock, Blocks.DIRT.defaultBlockState());

                check(
                        inMatch.player().gameMode.destroyBlock(matchBlock),
                        "The match player's dirt must break");
                check(
                        outside.player().gameMode.destroyBlock(outsideBlock),
                        "The other player's dirt must break");

                checkCount(
                        inMatch.player(),
                        Items.DIRT,
                        0,
                        "a match player whose match keeps drops in the world");
                check(
                        itemsNear(helper.getLevel(), matchBlock, Items.DIRT) == 1,
                        "The match player's dirt must stay in the world");
                checkCount(outside.player(), Items.DIRT, 1, "a player the general mod delivers to");
                check(
                        itemsNear(helper.getLevel(), outsideBlock, Items.DIRT) == 0,
                        "The other player's dirt must not stay in the world");
            } finally {
                DropPolicies.unregister(match);
                DropPolicies.unregister(general);
            }
        }
        helper.succeed();
    }

    public static void ungovernedPlayerKeepsDropsInWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcNobody", FIRST_POS)) {
            BlockPos block = helper.absolutePos(BLOCK_POS);
            helper.getLevel().setBlockAndUpdate(block, Blocks.DIRT.defaultBlockState());

            check(fixture.player().gameMode.destroyBlock(block), "The dirt must break");

            checkCount(fixture.player(), Items.DIRT, 0, "a player no policy governs");
            check(
                    itemsNear(helper.getLevel(), block, Items.DIRT) == 1,
                    "The dirt must stay in the world");
        }
        helper.succeed();
    }

    public static void refusedStackStaysWhereItSpawned(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcRefused", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            TestPolicy policy =
                    new TestPolicy(
                            Set.of(player.getUUID()), true, stack -> !stack.is(Items.EMERALD));
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos chest = helper.absolutePos(FAR_POS);
                helper.getLevel().setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
                ChestBlockEntity contents =
                        (ChestBlockEntity) helper.getLevel().getBlockEntity(chest);
                contents.setItem(0, new ItemStack(Items.EMERALD, 5));
                contents.setItem(1, new ItemStack(Items.DIAMOND, 3));

                check(player.gameMode.destroyBlock(chest), "The chest must break");

                checkCount(player, Items.DIAMOND, 3, "an accepted stack");
                checkCount(player, Items.CHEST, 1, "the accepted chest itself");
                checkCount(player, Items.EMERALD, 0, "a refused stack");
                check(
                        itemsNear(helper.getLevel(), chest, Items.EMERALD) == 5,
                        "The refused emeralds must stay where the chest was");
                check(
                        itemsNear(helper.getLevel(), player.blockPosition(), Items.EMERALD) == 0,
                        "The refused emeralds must not be moved to the player");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void blockBrokenForAPlayerElsewhereIsCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcElsewhere", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos block = helper.absolutePos(BLOCK_POS);
                helper.getLevel().setBlockAndUpdate(block, Blocks.DIRT.defaultBlockState());

                check(helper.getLevel().destroyBlock(block, true, player), "The dirt must break");

                checkCount(player, Items.DIRT, 1, "a block a mod broke naming the player");
                check(
                        itemsNear(helper.getLevel(), block, Items.DIRT) == 0,
                        "The dirt must not stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void felledLeafDecayPaysTheFeller(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcFeller", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos log = helper.absolutePos(BLOCK_POS);
                BlockPos felledLeaf = log.above();
                BlockPos otherLeaf = helper.absolutePos(new BlockPos(6, 1, 6));
                level.setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
                BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState();
                level.setBlockAndUpdate(felledLeaf, leaf);
                level.setBlockAndUpdate(otherLeaf, leaf);

                LeafOwners.felled(player, level, List.of(log));
                level.setBlockAndUpdate(log, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(felledLeaf, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(otherLeaf, Blocks.AIR.defaultBlockState());
                for (int roll = 0; roll < LEAF_ROLLS; roll++) {
                    Block.dropResources(leaf, level, felledLeaf);
                }

                int paid = leafDrops(player);
                check(paid > 0, "The felled tree's decaying leaf must pay its feller");
                check(
                        itemsNear(level, felledLeaf, null) == 0,
                        "No drop of the felled tree's leaf may stay on the ground");

                for (int roll = 0; roll < LEAF_ROLLS; roll++) {
                    Block.dropResources(leaf, level, otherLeaf);
                }
                check(leafDrops(player) == paid, "A leaf no felling recorded must not pay anyone");
                check(
                        itemsNear(level, otherLeaf, null) > 0,
                        "A leaf no felling recorded must drop in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void playerLogBreakRecordsItsLeaves(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcLogger", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            BlockPos log = helper.absolutePos(BLOCK_POS);
            BlockPos leafPos = log.above();
            try {
                BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState();
                level.setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
                level.setBlockAndUpdate(leafPos, leaf);

                check(player.gameMode.destroyBlock(log), "The log must break");
                checkCount(player, Items.OAK_LOG, 1, "the log the player broke");
                level.setBlockAndUpdate(leafPos, Blocks.AIR.defaultBlockState());
                for (int roll = 0; roll < LEAF_ROLLS; roll++) {
                    Block.dropResources(leaf, level, leafPos);
                }

                check(leafDrops(player) > 0, "The broken log's decaying leaf must pay the player");
                check(
                        itemsNear(level, leafPos, null) == 0,
                        "No drop of the broken log's leaf may stay on the ground");
            } finally {
                DropPolicies.unregister(policy);
                discardItems(level, log);
            }
        }
        helper.succeed();
    }

    public static void respawnAnchorClickCreditsTheClicker(GameTestHelper helper) {
        clickExplodes(
                helper,
                "DcAnchor",
                0,
                (end, foot) ->
                        end.setBlock(
                                foot,
                                Blocks.RESPAWN_ANCHOR
                                        .defaultBlockState()
                                        .setValue(RespawnAnchorBlock.CHARGE, 4),
                                Block.UPDATE_CLIENTS));
    }

    public static void bedClickCreditsTheClicker(GameTestHelper helper) {
        clickExplodes(
                helper,
                "DcBed",
                32,
                (end, foot) -> {
                    BlockState bed =
                            Blocks.BED
                                    .pick(net.minecraft.world.item.DyeColor.RED)
                                    .defaultBlockState()
                                    .setValue(BedBlock.FACING, Direction.EAST);
                    end.setBlock(
                            foot, bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
                    end.setBlock(
                            foot.east(),
                            bed.setValue(BedBlock.PART, BedPart.HEAD),
                            Block.UPDATE_CLIENTS);
                });
    }

    /// Builds a closed room in a force-loaded chunk of the End, far from its islands, places the
    /// block that explodes at its centre and an emerald chest beside it, and once the room has
    /// loaded, has a player standing three blocks away click the block. Entity-ticking status is
    /// how a forced chunk shows it has loaded (ServerLevel.java:1929); an explosion reaches the
    /// entities of every loaded chunk, ticking or not (ServerExplosion.java:182,
    /// Visibility.java:22-30).
    private static void clickExplodes(
            GameTestHelper helper,
            String name,
            int roomOffset,
            java.util.function.BiConsumer<ServerLevel, BlockPos> place) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        check(end != null, "The test server must have an End");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos room =
                new ChunkPos((origin.getX() + 20_000) >> 4, (origin.getZ() + roomOffset) >> 4);
        BlockPos foot = new BlockPos(room.getMinBlockX() + 8, 64, room.getMinBlockZ() + 8);
        BlockPos chest = foot.west(2);
        GameTestPlayer fixture = player(helper, name, FIRST_POS);
        ServerPlayer player = fixture.player();
        TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
        Runnable cleanup =
                () -> {
                    DropPolicies.unregister(policy);
                    discardItems(end, foot);
                    end.setChunkForced(room.x(), room.z(), false);
                    fixture.close();
                };
        Block clicked;
        try {
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            end.setChunkForced(room.x(), room.z(), true);
            clearRoom(end, foot);
            place.accept(end, foot);
            clicked = end.getBlockState(foot).getBlock();
            placeEmeraldChest(end, chest);
            player.teleportTo(
                    end,
                    foot.getX() + 0.5D,
                    foot.getY(),
                    foot.getZ() + 3.5D,
                    Set.of(),
                    0.0F,
                    0.0F,
                    true);
            // What the client's teleport confirmation would do: a player still changing dimension
            // takes no damage.
            player.hasChangedDimension();
            fixture.markLoaded();
            toughen(player);
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        end.isPositionEntityTicking(foot),
                                        "The End room must tick entities"))
                .thenExecute(
                        () -> {
                            try {
                                fixture.clearMessages();

                                click(player, end, foot);

                                // The blast sets fire, which may take the clicked block's place.
                                check(
                                        !end.getBlockState(foot).is(clicked),
                                        "The clicked block must explode");
                                checkCount(
                                        player,
                                        Items.EMERALD,
                                        64,
                                        "a chest the clicked block's blast broke");
                                check(
                                        itemsNear(end, chest, Items.EMERALD) == 0,
                                        "No emerald may stay where the chest was");
                                // A blast that named the clicker as its source would leave them out
                                // of its entity step; the game's own blast still hits them, so
                                // their client is sent its knockback, as it always was.
                                check(blastHit(fixture), "The blast must still reach the clicker");
                            } finally {
                                cleanup.run();
                            }
                        })
                .thenSucceed();
    }

    private static GameTestPlayer player(GameTestHelper helper, String name, BlockPos position) {
        GameTestPlayer fixture =
                GameTestPlayer.join(helper, name)
                        .markLoaded()
                        .gameMode(GameType.SURVIVAL)
                        .moveTo(helper, position);
        fixture.player().getInventory().clearContent();
        return fixture;
    }

    private static void toughen(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 3));
    }

    private static void click(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.useItemOn(
                player,
                level,
                ItemStack.EMPTY,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static boolean blastHit(GameTestPlayer fixture) {
        EmbeddedChannel channel = fixture.channel();
        channel.runPendingTasks();
        channel.flushOutbound();
        boolean hit = false;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            hit |=
                    outbound instanceof ClientboundExplodePacket explosion
                            && explosion.playerKnockback().isPresent();
            ReferenceCountUtil.release(outbound);
        }
        return hit;
    }

    private static void placeEmeraldChest(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.EMERALD, 64));
    }

    private static void clearRoom(ServerLevel level, BlockPos foot) {
        for (BlockPos pos : BlockPos.betweenClosed(foot.offset(-6, -1, -6), foot.offset(6, 6, 6))) {
            boolean shell =
                    pos.getY() == foot.getY() - 1
                            || Math.abs(pos.getX() - foot.getX()) == 6
                            || Math.abs(pos.getZ() - foot.getZ()) == 6
                            || pos.getY() == foot.getY() + 6;
            level.setBlock(
                    pos,
                    shell ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    private static void discardItems(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8.0D))
                .forEach(ItemEntity::discard);
    }

    private static int leafDrops(ServerPlayer player) {
        return player.getInventory().countItem(Items.OAK_SAPLING)
                + player.getInventory().countItem(Items.STICK)
                + player.getInventory().countItem(Items.APPLE);
    }

    private static int itemsNear(ServerLevel level, BlockPos pos, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D)).stream()
                .filter(entity -> item == null || entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void checkCount(ServerPlayer player, Item item, int expected, String what) {
        int actual = player.getInventory().countItem(item);
        check(
                actual == expected,
                "Expected "
                        + expected
                        + " "
                        + item
                        + " in the inventory for "
                        + what
                        + ", got "
                        + actual);
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private record TestPolicy(Set<UUID> players, boolean deliver, Predicate<ItemStack> filter)
            implements DropPolicy {
        TestPolicy(Set<UUID> players, boolean deliver) {
            this(players, deliver, stack -> true);
        }

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
            return filter.test(stack);
        }
    }
}
