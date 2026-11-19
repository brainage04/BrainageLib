package io.github.brainage04.brainagelib.drops;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.ticks.LevelTicks;
import org.jspecify.annotations.Nullable;

/**
 * Which player's break scheduled the block tick at a position, for the blocks that break a tick
 * after their support goes.
 *
 * <p>Sugar cane, cactus, bamboo, kelp, cave, weeping and twisting vines, chorus plants and flowers,
 * big dripleaf stems and scaffolding do not break with their support: their {@code updateShape}
 * schedules a tick and their {@code tick} then calls {@code level.destroyBlock(pos, true)} with no
 * breaker ({@code SugarCaneBlock.java:37-38,74-75}, {@code CactusBlock.java:41-42,98-99}, {@code
 * BambooStalkBlock.java:100-101,138-139}, {@code GrowingPlantBlock.java:55-56} with {@code
 * GrowingPlantBodyBlock.java:42-43}, {@code ChorusPlantBlock.java:64-65,77-78}, {@code
 * BigDripleafStemBlock.java:79-80,92-93}, {@code ScaffoldingBlock.java:103,115-117}). Those drops
 * are outside every capture scope, so they land on the ground.
 *
 * <p>So a block tick scheduled while a player's break is running is recorded for that player, and
 * {@code ServerLevel.tickBlock} runs a recorded position's tick inside that player's scope ({@link
 * DropCapture}). A column carries the player from tick to tick, because the next block's tick is
 * scheduled inside the scope the previous one ran in. Only a player's own break records: an
 * explosion, a mob's death, a decaying leaf, a piston, flowing water or another player's break
 * schedules ticks with no break running, so those drops stay where the game puts them.
 *
 * <p>The window is short because the tick it records runs on the next game tick; it only has to
 * outlast a chunk that stops ticking between the two. A record is used up by the tick it names.
 */
public final class SupportTicks {
    /** Ticks a record waits for its tick: the tick is due next tick, and a stalled chunk may be late. */
    private static final int WINDOW = 100;

    private static final int LIMIT = 65_536;

    private record Owner(UUID player, Block type, long expires) {}

    /**
     * By the level's own tick list, not by dimension: a regenerated world has a new {@link
     * LevelTicks}, and must not keep the old one's records for the 65 thousand entries it still
     * allows.
     */
    private static final Map<LevelTicks<Block>, Long2ObjectOpenHashMap<Owner>> TICKS =
            new IdentityHashMap<>();

    /** The players whose breaks are running on this thread, innermost last. */
    private static final ThreadLocal<ArrayDeque<ServerPlayer>> BREAKERS = new ThreadLocal<>();

    private SupportTicks() {}

    /**
     * Starts a block break for {@code player}: drops spawned until {@link #exit} go to them, and
     * every block tick scheduled until then is theirs.
     */
    public static void enter(ServerPlayer player) {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            breakers = new ArrayDeque<>();
            BREAKERS.set(breakers);
        }
        breakers.addLast(player);
        DropCapture.begin(player);
    }

    /** Ends the innermost block break. */
    public static void exit() {
        DropCapture.end();
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            return;
        }
        breakers.removeLast();
        if (breakers.isEmpty()) {
            BREAKERS.remove();
        }
    }

    /**
     * Records that the break running on this thread scheduled the tick of {@code type} at {@code
     * pos} in {@code ticks}, so its drops go to that player. Called for every block tick scheduled
     * on a server level.
     */
    public static void record(LevelTicks<Block> ticks, BlockPos pos, Block type) {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            return;
        }
        ServerPlayer player = breakers.getLast();
        if (!(player.level() instanceof ServerLevel level)
                || level.getBlockTicks() != ticks
                || !player.isAlive()) {
            return;
        }
        Long2ObjectOpenHashMap<Owner> records =
                TICKS.computeIfAbsent(ticks, key -> new Long2ObjectOpenHashMap<>());
        records.put(pos.asLong(), new Owner(player.getUUID(), type, level.getGameTime() + WINDOW));
        if (records.size() > LIMIT) {
            prune(records, level.getGameTime());
        }
    }

    /**
     * The player whose break scheduled the tick of {@code type} at {@code pos}, if a record in its
     * window names one and they are still here. The record is used up.
     */
    public static @Nullable ServerPlayer owner(ServerLevel level, BlockPos pos, Block type) {
        Long2ObjectOpenHashMap<Owner> records = TICKS.get(level.getBlockTicks());
        if (records == null) {
            return null;
        }
        Owner owner = records.get(pos.asLong());
        if (owner == null || owner.type() != type) {
            return null;
        }
        records.remove(pos.asLong());
        if (owner.expires() < level.getGameTime()) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner.player());
        return player != null && player.level() == level && player.isAlive() ? player : null;
    }

    /**
     * Forgets every record. Called when a server stops: a record's window is in its world's game
     * time, so it must not reach a world the same JVM loads next.
     */
    public static void clear() {
        TICKS.clear();
    }

    private static void prune(Long2ObjectOpenHashMap<Owner> records, long now) {
        LongIterator keys = records.keySet().iterator();
        while (keys.hasNext()) {
            if (records.get(keys.nextLong()).expires() < now) {
                keys.remove();
            }
        }
    }
}
