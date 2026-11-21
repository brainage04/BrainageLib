package io.github.brainage04.brainagelib.drops;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
 * <p>So a block tick scheduled while a player's break is running is recorded for that player, and a
 * {@code Level.destroyBlock} with no breaker at a recorded position runs inside that player's scope
 * ({@link DropCapture}). The scope covers the whole call, including the {@code setBlock} that
 * schedules the next block's tick, so a column carries the player from tick to tick. Only a
 * player's own break records: an explosion, a mob's death, a decaying leaf, a piston, flowing water
 * or another player's break schedules ticks with no break running, so those drops stay where the
 * game puts them.
 *
 * <p>The record is used only by a destroy that actually happens, at that position, of that block,
 * with no breaker. The {@code canSurvive} test at record time is a narrowing, not the decision: it
 * keeps the map to blocks that have already lost their support, which is the family's precondition.
 * It can still record a tick that will not destroy, such as a scaffolding block that falls instead
 * of breaking ({@code ScaffoldingBlock.java:113-120}) or a repeater that drops in {@code
 * neighborChanged} rather than in a tick ({@code DiodeBlock.java:74-75}); that path calls {@code
 * removeBlock}, not {@code destroyBlock}, so the record is never read and no scope opens.
 */
public final class SupportTicks {
    /**
     * Ticks a record waits for its tick. Every block in the family schedules with a delay of 1
     * ({@code SugarCaneBlock.java:75}, {@code CactusBlock.java:99}, {@code BambooStalkBlock.java:139},
     * {@code GrowingPlantBodyBlock.java:43}, {@code GrowingPlantHeadBlock.java:84},
     * {@code ChorusPlantBlock.java:65}, {@code ChorusFlowerBlock.java:149},
     * {@code BigDripleafStemBlock.java:80}, {@code ScaffoldingBlock.java:103}), and a tick whose
     * trigger has passed runs on the first tick its chunk is tickable again
     * ({@code LevelTicks.java:107-119}), with no bound of its own. This is a chosen bound, not a
     * game rule: a tick later than this loses its record and its drops stay in the world.
     */
    private static final int WINDOW = 100;

    private static final int LIMIT = 65_536;

    private record Owner(UUID player, Block type, long expires) {}

    /**
     * By dimension, so a world reload reuses one map instead of adding one per {@link ServerLevel};
     * {@link #clear} on server stop empties it, and a record's window is in its world's game time.
     */
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Owner>> TICKS =
            new HashMap<>();

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
     * pos} in {@code ticks}, so a destroy of that block there pays that player. Called for every
     * block tick scheduled on a server level.
     */
    public static void record(LevelTicks<Block> ticks, BlockPos pos, Block type) {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            return;
        }
        ServerPlayer player = breakers.getLast();
        if (!(player.level() instanceof ServerLevel level)
                || level.getBlockTicks() != ticks
                || !player.isAlive()
                || player.preventsBlockDrops()) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(type) || state.canSurvive(level, pos)) {
            return;
        }
        Long2ObjectOpenHashMap<Owner> records =
                TICKS.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>());
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
        Long2ObjectOpenHashMap<Owner> records = TICKS.get(level.dimension());
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
