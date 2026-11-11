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
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.DyeColor;
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

/// Each policy governs only the players of its test, so tests running side by side never decide for
/// each other.
public final class DropCaptureGameTests {
    private static final BlockPos FIRST_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND_POS = new BlockPos(5, 1, 1);
    private static final BlockPos BLOCK_POS = new BlockPos(3, 1, 3);
    private static final BlockPos FAR_POS = new BlockPos(3, 1, 10);
    private static final BlockPos MOB_POS = new BlockPos(3, 1, 6);

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
                discardItems(level, helper.getBounds());
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

    public static void armorStandBrokenByPlayerIsCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcStand", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos at = helper.absolutePos(BLOCK_POS);
                ArmorStand stand =
                        new ArmorStand(level, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
                stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                check(level.addFreshEntity(stand), "The stand must spawn");

                punchStandTwice(stand, level, player);

                check(stand.isRemoved(), "The second punch must break the stand");
                checkCount(player, Items.ARMOR_STAND, 1, "a stand a player broke");
                checkCount(player, Items.IRON_HELMET, 1, "a broken stand's helmet");
                checkCount(player, Items.DIAMOND_SWORD, 1, "a broken stand's sword");
                check(
                        itemsNear(level, at, null) == 0,
                        "Nothing of a stand a player broke may stay on the ground");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A blast that breaks a stand names no player, so the stand's equipment stays where it broke.
    /// That path never drops the stand itself.
    public static void armorStandBrokenByABlastStaysInTheWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcBlastStand", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos at = helper.absolutePos(BLOCK_POS);
                ArmorStand stand =
                        new ArmorStand(level, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
                stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                check(level.addFreshEntity(stand), "The stand must spawn");

                DamageSource blast = level.damageSources().explosion((Entity) null, (Entity) null);
                stand.hurtServer(level, blast, 1.0F);

                check(stand.isRemoved(), "A blast must break the stand");

                checkCount(player, Items.IRON_HELMET, 0, "a stand a blast broke");
                checkCount(player, Items.ARMOR_STAND, 0, "a stand a blast broke");
                check(
                        itemsNear(level, at.above(), Items.IRON_HELMET) == 1,
                        "The blast-broken stand's helmet must stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A blast the player caused names them, so a stand it breaks still hands them its equipment.
    public static void armorStandBrokenByAPlayersBlastIsCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcOwnBlast", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos at = helper.absolutePos(BLOCK_POS);
                ArmorStand stand =
                        new ArmorStand(level, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
                stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                check(level.addFreshEntity(stand), "The stand must spawn");
                PrimedTnt tnt = new PrimedTnt(level, at.getX(), at.getY(), at.getZ(), player);

                DamageSource blast = level.damageSources().explosion(tnt, player);
                stand.hurtServer(level, blast, 1.0F);

                check(stand.isRemoved(), "The player's blast must break the stand");

                checkCount(player, Items.IRON_HELMET, 1, "a stand the player's own blast broke");
                check(
                        itemsNear(level, at.above(), Items.IRON_HELMET) == 0,
                        "A stand the player's blast broke must not leave its helmet behind");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A cushion is a block-attached decoration too, so the same scope hands its item to the player
    /// who breaks it.
    public static void cushionBrokenByPlayerIsCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcCushion", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos at = helper.absolutePos(BLOCK_POS);
                Cushion cushion = new Cushion(EntityTypes.CUSHION, level);
                cushion.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
                check(level.addFreshEntity(cushion), "The cushion must spawn");

                check(
                        cushion.hurtServer(level, level.damageSources().playerAttack(player), 0.0F),
                        "The punch must break the cushion");

                check(cushion.isRemoved(), "The broken cushion must be removed");
                checkCount(
                        player, Items.CUSHION.pick(DyeColor.WHITE), 1, "a cushion a player broke");
                check(
                        itemsNear(level, at, Items.CUSHION.pick(DyeColor.WHITE)) == 0,
                        "A broken cushion must not stay on the ground");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A punch knocks the framed item out first; the next one breaks the frame. Both belong to the
    /// player, for a plain and a glowing frame.
    public static void itemFramesBrokenByPlayerAreCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcFrames", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos plainWall = helper.absolutePos(BLOCK_POS);
                ItemFrame frame =
                        hangingFrame(
                                level,
                                plainWall,
                                new ItemFrame(level, plainWall.north(), Direction.NORTH),
                                Items.DIAMOND);
                punchFrameTwice(frame, level, player);
                check(frame.isRemoved(), "The second punch must break the frame");
                checkCount(player, Items.DIAMOND, 1, "the item a punch knocked out of a frame");
                checkCount(player, Items.ITEM_FRAME, 1, "an item frame a player broke");
                check(
                        itemsNear(level, plainWall.north(), null) == 0,
                        "Nothing of a frame a player broke may stay on the ground");

                BlockPos glowWall = helper.absolutePos(MOB_POS);
                ItemFrame glow =
                        hangingFrame(
                                level,
                                glowWall,
                                new GlowItemFrame(level, glowWall.north(), Direction.NORTH),
                                Items.EMERALD);
                punchFrameTwice(glow, level, player);
                check(glow.isRemoved(), "The second punch must break the glow frame");
                checkCount(
                        player, Items.EMERALD, 1, "the item a punch knocked out of a glow frame");
                checkCount(player, Items.GLOW_ITEM_FRAME, 1, "a glow item frame a player broke");
                check(
                        itemsNear(level, glowWall.north(), null) == 0,
                        "Nothing of a glow frame a player broke may stay on the ground");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void paintingBrokenByPlayerIsCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcPainting", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                BlockPos wall = helper.absolutePos(BLOCK_POS);
                level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                Painting painting =
                        new Painting(
                                level,
                                wall.north(),
                                Direction.NORTH,
                                VariantUtils.getAny(
                                        level.registryAccess(), Registries.PAINTING_VARIANT));
                check(level.addFreshEntity(painting), "The painting must spawn");

                DamageSource punch = level.damageSources().playerAttack(player);
                check(painting.hurtServer(level, punch, 0.0F), "The punch must break the painting");

                check(painting.isRemoved(), "The broken painting must be removed");
                checkCount(player, Items.PAINTING, 1, "a painting a player broke");
                check(
                        itemsNear(level, wall.north(), Items.PAINTING) == 0,
                        "A broken painting must not stay on the ground");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void decorationBrokenWithoutAPolicyStaysInTheWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcNoPolicy", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos wall = helper.absolutePos(BLOCK_POS);
            ItemFrame frame =
                    hangingFrame(
                            level,
                            wall,
                            new ItemFrame(level, wall.north(), Direction.NORTH),
                            Items.DIAMOND);

            punchFrameTwice(frame, level, player);

            checkCount(player, Items.DIAMOND, 0, "a player no policy governs");
            checkCount(player, Items.ITEM_FRAME, 0, "a player no policy governs");
            check(
                    itemsNear(level, wall.north(), Items.DIAMOND) == 1,
                    "The punched-out item must stay in the world");
            check(
                    itemsNear(level, wall.north(), Items.ITEM_FRAME) == 1,
                    "The broken frame must stay in the world");
        }
        helper.succeed();
    }

    /// A minigame's match policy governs its players above a general mod's, and the entity scopes
    /// are decided by the same policy lookup as the block ones.
    public static void matchPolicyGovernsDecorationBreaks(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcMatchFrame", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy general = new TestPolicy(Set.of(player.getUUID()), true);
            TestPolicy match = new TestPolicy(Set.of(player.getUUID()), false);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, general);
            DropPolicies.register(DropPolicies.MATCH_PRIORITY, match);
            try {
                BlockPos wall = helper.absolutePos(BLOCK_POS);
                ItemFrame frame =
                        hangingFrame(
                                level,
                                wall,
                                new ItemFrame(level, wall.north(), Direction.NORTH),
                                Items.DIAMOND);

                punchFrameTwice(frame, level, player);

                checkCount(player, Items.DIAMOND, 0, "a player in a match that keeps drops");
                checkCount(player, Items.ITEM_FRAME, 0, "a player in a match that keeps drops");
                check(
                        itemsNear(level, wall.north(), Items.DIAMOND) == 1,
                        "A match player's punched-out item must stay in the world");
                check(
                        itemsNear(level, wall.north(), Items.ITEM_FRAME) == 1,
                        "A match player's broken frame must stay in the world");
            } finally {
                DropPolicies.unregister(match);
                DropPolicies.unregister(general);
            }
        }
        helper.succeed();
    }

    /// The game's kill credit can name a player through its last-hurt record while its own 100-tick
    /// player timer is out; the kill statistic and advancement still go to that player.
    public static void killCreditedWithoutAPlayerHitGoesToTheKiller(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcCredit", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                Husk husk = gearedHusk(helper, MOB_POS);
                husk.setLastHurtByMob(player);
                check(
                        husk.getLastHurtByPlayerMemoryTime() == 0,
                        "The husk must have no player hit on its timer");

                husk.kill(level);

                checkCount(player, Items.IRON_HELMET, 1, "a kill the game credits to the player");
                check(
                        itemsNear(level, helper.absolutePos(MOB_POS).above(), Items.IRON_HELMET)
                                == 0,
                        "A credited kill's helmet must not stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    public static void killNoPlayerIsCreditedWithStaysInTheWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcUncredited", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                Husk husk = gearedHusk(helper, MOB_POS);
                check(husk.getLastHurtByMob() == null, "The husk must have been hurt by nobody");

                husk.kill(level);

                checkCount(player, Items.IRON_HELMET, 0, "a kill nobody is credited with");
                check(
                        itemsNear(level, helper.absolutePos(MOB_POS).above(), Items.IRON_HELMET)
                                == 1,
                        "An uncredited kill's helmet must stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A vehicle a player breaks hands over its own item, and a chest boat, chest minecart or
    /// hopper minecart its contents with it, like any other container.
    public static void vehiclesBrokenByPlayerAreCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcVehicles", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                Boat boat = helper.spawn(EntityTypes.OAK_BOAT, BLOCK_POS);
                breakVehicle(boat, player);
                checkCount(player, Items.OAK_BOAT, 1, "a boat a player broke");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.OAK_BOAT) == 0,
                        "A broken boat's item must not stay in the world");

                player.getInventory().clearContent();
                ChestBoat chestBoat = helper.spawn(EntityTypes.OAK_CHEST_BOAT, BLOCK_POS);
                chestBoat.setItem(0, new ItemStack(Items.EMERALD, 3));
                breakVehicle(chestBoat, player);
                checkCount(player, Items.OAK_CHEST_BOAT, 1, "a chest boat a player broke");
                checkCount(player, Items.EMERALD, 3, "the contents of a chest boat");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), null) == 0,
                        "Nothing of a chest boat a player broke may stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// The same rule for every minecart. A furnace minecart has no contents to hand over, and a
    /// TNT minecart drops its item for a plain punch, because only fire and blasts prime it.
    public static void minecartsBrokenByPlayerAreCaptured(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcMinecarts", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                Minecart minecart = helper.spawn(EntityTypes.MINECART, BLOCK_POS);
                breakVehicle(minecart, player);
                checkCount(player, Items.MINECART, 1, "a minecart a player broke");

                player.getInventory().clearContent();
                MinecartChest chestCart = helper.spawn(EntityTypes.CHEST_MINECART, BLOCK_POS);
                chestCart.setItem(0, new ItemStack(Items.EMERALD, 2));
                breakVehicle(chestCart, player);
                checkCount(player, Items.CHEST_MINECART, 1, "a chest minecart a player broke");
                checkCount(player, Items.EMERALD, 2, "the contents of a chest minecart");

                player.getInventory().clearContent();
                MinecartHopper hopperCart = helper.spawn(EntityTypes.HOPPER_MINECART, BLOCK_POS);
                hopperCart.setItem(0, new ItemStack(Items.DIAMOND, 4));
                breakVehicle(hopperCart, player);
                checkCount(player, Items.HOPPER_MINECART, 1, "a hopper minecart a player broke");
                checkCount(player, Items.DIAMOND, 4, "the contents of a hopper minecart");

                player.getInventory().clearContent();
                MinecartFurnace furnaceCart = helper.spawn(EntityTypes.FURNACE_MINECART, BLOCK_POS);
                breakVehicle(furnaceCart, player);
                checkCount(player, Items.FURNACE_MINECART, 1, "a furnace minecart a player broke");

                player.getInventory().clearContent();
                MinecartTNT tntCart = helper.spawn(EntityTypes.TNT_MINECART, BLOCK_POS);
                // One hit that breaks it while it stands still: a moving or burning TNT minecart
                // primes instead of dropping, which is vanilla's own rule and not a break.
                check(
                        tntCart.hurtServer(
                                level, level.damageSources().playerAttack(player), 10.0F),
                        "The hit must break the TNT minecart");
                checkCount(player, Items.TNT_MINECART, 1, "a TNT minecart a player broke");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), null) == 0,
                        "Nothing of a vehicle a player broke may stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A vehicle that breaks without a player cause (lava here, a fire or a mob in play) keeps its
    /// item and contents in the world.
    public static void vehicleBrokenWithoutAPlayerStaysInTheWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcVehicleLava", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                MinecartChest cart = helper.spawn(EntityTypes.CHEST_MINECART, BLOCK_POS);
                cart.setItem(0, new ItemStack(Items.EMERALD, 3));

                check(
                        cart.hurtServer(level, level.damageSources().lava(), 100.0F),
                        "Lava must break the chest minecart");

                check(cart.isRemoved(), "The lava must remove the chest minecart");
                checkCount(player, Items.CHEST_MINECART, 0, "a minecart lava broke");
                checkCount(player, Items.EMERALD, 0, "the contents of a minecart lava broke");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.CHEST_MINECART) == 1,
                        "The lava-broken minecart's item must stay in the world");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.EMERALD) == 3,
                        "The lava-broken minecart's contents must stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A creative player's punch discards a vehicle, as it always did; the contents a chest boat
    /// drops on the way out stay in the world.
    public static void creativeBreakOfVehicleStaysInTheWorld(GameTestHelper helper) {
        try (GameTestPlayer fixture =
                GameTestPlayer.join(helper, "DcVehicleCreative")
                        .markLoaded()
                        .gameMode(GameType.CREATIVE)
                        .moveTo(helper, FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), true);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                player.getInventory().clearContent();
                ChestBoat chestBoat = helper.spawn(EntityTypes.OAK_CHEST_BOAT, BLOCK_POS);
                chestBoat.setItem(0, new ItemStack(Items.EMERALD, 3));

                player.attack(chestBoat);

                check(chestBoat.isRemoved(), "A creative punch must discard the chest boat");
                checkCount(player, Items.OAK_CHEST_BOAT, 0, "a creative player");
                checkCount(player, Items.EMERALD, 0, "the contents of a creative break");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.EMERALD) == 3,
                        "The chest boat's contents must stay in the world for a creative break");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A policy that governs the player but does not deliver (a match, or Telekinesis switched off)
    /// keeps a broken vehicle where the game dropped it.
    public static void vehicleBrokenByAPlayerWhoIsNotDeliveredStaysInTheWorld(
            GameTestHelper helper) {
        try (GameTestPlayer fixture = player(helper, "DcVehicleOff", FIRST_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            TestPolicy policy = new TestPolicy(Set.of(player.getUUID()), false);
            DropPolicies.register(DropPolicies.DEFAULT_PRIORITY, policy);
            try {
                MinecartChest cart = helper.spawn(EntityTypes.CHEST_MINECART, BLOCK_POS);
                cart.setItem(0, new ItemStack(Items.EMERALD, 3));

                breakVehicle(cart, player);

                check(cart.isRemoved(), "The punches must break the chest minecart");
                checkCount(player, Items.CHEST_MINECART, 0, "a player a policy does not deliver to");
                checkCount(player, Items.EMERALD, 0, "a player a policy does not deliver to");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.CHEST_MINECART) == 1,
                        "The broken minecart's item must stay in the world");
                check(
                        itemsNear(level, helper.absolutePos(BLOCK_POS), Items.EMERALD) == 3,
                        "The broken minecart's contents must stay in the world");
            } finally {
                DropPolicies.unregister(policy);
            }
        }
        helper.succeed();
    }

    /// A vehicle breaks once its accumulated damage passes 40, and a punch deals a fifth of its
    /// damage while the attack cooldown runs, so keep punching rather than counting hits.
    private static void breakVehicle(VehicleEntity vehicle, ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        for (int punch = 0; punch < 40 && !vehicle.isRemoved(); punch++) {
            player.attack(vehicle);
        }
        check(vehicle.isRemoved(), "The player's punches must break the vehicle");
    }

    /// Two punches in one tick. The first only shakes the stand, because `ArmorStand.hurtServer`
    /// refuses to break one again within five ticks of the last hit unless the damage type always
    /// kills it; the second breaks it and drops the stand and its equipment.
    private static void punchStandTwice(ArmorStand stand, ServerLevel level, ServerPlayer player) {
        DamageSource punch = level.damageSources().playerAttack(player);
        stand.hurtServer(level, punch, 1.0F);
        stand.hurtServer(level, punch, 1.0F);
    }

    private static ItemFrame hangingFrame(
            ServerLevel level, BlockPos wall, ItemFrame frame, Item content) {
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        frame.setItem(new ItemStack(content));
        check(level.addFreshEntity(frame), "The " + content + " frame must spawn");
        return frame;
    }

    private static void punchFrameTwice(ItemFrame frame, ServerLevel level, ServerPlayer player) {
        DamageSource punch = level.damageSources().playerAttack(player);
        check(frame.hurtServer(level, punch, 0.0F), "The first punch must knock the item out");
        check(frame.hurtServer(level, punch, 0.0F), "The second punch must break the frame");
    }

    /// A husk with a helmet that always drops, so its death's equipment is exact: the loot table's
    /// random drops do not matter.
    private static Husk gearedHusk(GameTestHelper helper, BlockPos relative) {
        Husk husk = helper.spawn(EntityTypes.HUSK, relative);
        husk.setNoAi(true);
        husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        husk.setDropChance(EquipmentSlot.HEAD, 2.0F);
        return husk;
    }

    /// Builds a closed room in a force-loaded chunk of the End, far from its islands, places the
    /// block that explodes at its centre and an emerald chest beside it, and once the room has
    /// loaded, has a player standing three blocks away click the block. A forced chunk loads to
    /// entity-ticking status (ChunkMap.java:128), the status the wait checks
    /// (ServerLevel.java:1929); an explosion reaches the entities of every loaded chunk, ticking or
    /// not (ServerExplosion.java:182, Visibility.java:22-30).
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
                    discardItems(end, roomAt(foot));
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

    private static AABB roomAt(BlockPos foot) {
        return new AABB(
                foot.getX() - 6.0D,
                foot.getY() - 1.0D,
                foot.getZ() - 6.0D,
                foot.getX() + 7.0D,
                foot.getY() + 7.0D,
                foot.getZ() + 7.0D);
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

    /// Items inside the area only: the server lays tests out 8 to a row, five blocks apart
    /// (GameTestServer.java:325, StructureGridSpawner.java:17-18), so a radius that reaches past
    /// the test's own bounds can delete a neighbour's drops.
    private static void discardItems(ServerLevel level, AABB area) {
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
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
