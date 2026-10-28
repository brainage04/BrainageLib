package io.github.brainage04.brainagelib.drops;

import io.github.brainage04.brainagelib.mixin.ExperienceOrbAccessor;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sends the items and experience spawned inside a capture scope to the scope's player, when the
 * {@link DropPolicies} deliver to them.
 *
 * <p>A scope covers one player block break, one mob's death loot credited to a player, the block
 * drops of one explosion a player caused, or a decaying leaf of a tree a player felled ({@link
 * LeafOwners}). Scopes nest; the innermost one decides. A mob death or explosion without a player
 * cause opens no scope, so inside a block break its drops still go to the breaker. A player's own
 * death drops open a scope for nobody (a {@code null} player), which keeps any outer scope from
 * handing that inventory to someone else.
 *
 * <p>What fits goes into the inventory; the rest is moved to the player's feet. A stack the
 * governing policy refuses ({@link DropPolicy#accepts}) stays where the game spawned it. Experience
 * repairs Mending items first and the rest is given as points.
 */
public final class DropCapture {
    private static final ThreadLocal<ArrayList<@Nullable ServerPlayer>> RECIPIENTS =
            new ThreadLocal<>();

    /**
     * The player whose click is causing an explosion that names nobody: a bed or respawn anchor
     * they used.
     */
    private static final ThreadLocal<ServerPlayer> CLICK = new ThreadLocal<>();

    private DropCapture() {}

    /**
     * Opens a scope for {@code player}, or for nobody with {@code null}. Close it with {@link
     * #end}.
     */
    public static void begin(@Nullable ServerPlayer player) {
        ArrayList<@Nullable ServerPlayer> recipients = RECIPIENTS.get();
        if (recipients == null) {
            recipients = new ArrayList<>();
            RECIPIENTS.set(recipients);
        }
        recipients.add(player);
    }

    /** Closes the innermost scope. */
    public static void end() {
        ArrayList<@Nullable ServerPlayer> recipients = RECIPIENTS.get();
        recipients.removeLast();
        if (recipients.isEmpty()) {
            RECIPIENTS.remove();
        }
    }

    /**
     * Runs {@code body} with {@code player} credited for the next explosion inside it that names no
     * player: the bed or respawn anchor a player used. Only that explosion's block step reads it
     * ({@link #explosionCause}), so the explosion's damage, knockback and kill credit stay the
     * game's own.
     */
    public static <T> T credited(ServerPlayer player, Supplier<T> body) {
        ServerPlayer previous = CLICK.get();
        CLICK.set(player);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                CLICK.remove();
            } else {
                CLICK.set(previous);
            }
        }
    }

    /**
     * The player the game credits with killing {@code victim}, or {@code null}: the player who hurt
     * it within the last 100 ticks, directly, with a projectile or damaging potion they own, or
     * through their tamed wolf. Vanilla uses the same record, with the same {@code
     * lastHurtByPlayerMemoryTime > 0} test, for player-kill loot and experience.
     */
    public static @Nullable ServerPlayer mobKiller(LivingEntity victim) {
        if (victim.getLastHurtByPlayerMemoryTime() <= 0) {
            return null;
        }
        return victim.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
    }

    /**
     * The player who caused {@code explosion}: its indirect source (the player who lit the TNT, or
     * who owns the projectile or fireball that exploded), else the attacker in its damage source
     * (the player who hit an end crystal), else the player who used the bed or respawn anchor that
     * made it. That last credit is used up here, so it pays exactly one explosion. Creepers and TNT
     * nobody lit have no player cause ({@code null}).
     */
    public static @Nullable ServerPlayer explosionCause(ServerExplosion explosion) {
        if (explosion.getIndirectSourceEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (explosion.getDamageSource().getEntity() instanceof ServerPlayer player) {
            return player;
        }
        ServerPlayer clicked = CLICK.get();
        if (clicked == null || clicked.level() != explosion.level()) {
            return null;
        }
        CLICK.remove();
        return clicked;
    }

    /**
     * Delivers a spawning item entity; true when all of it went to the scope's player and it must
     * not spawn.
     */
    public static boolean capture(ServerLevel level, ItemEntity itemEntity) {
        ServerPlayer player = activePlayer(level);
        if (player == null) {
            return false;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || !DropPolicies.accepts(player, stack)) {
            return false;
        }

        Item item = stack.getItem();
        int originalCount = stack.getCount();
        player.getInventory().add(stack);

        int insertedCount = originalCount - stack.getCount();
        if (insertedCount > 0) {
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), insertedCount);
        }
        if (stack.isEmpty()) {
            return true;
        }

        itemEntity.setPos(player.getX(), player.getY(), player.getZ());
        itemEntity.setDeltaMovement(Vec3.ZERO);
        return false;
    }

    /** Delivers experience spawning as an orb or award; true when it went to the scope's player. */
    public static boolean captureExperience(ServerLevel level, int amount) {
        ServerPlayer player = activePlayer(level);
        if (player == null || amount <= 0) {
            return false;
        }

        ExperienceOrb orb = new ExperienceOrb(level, player.position(), Vec3.ZERO, amount);
        int remaining = ((ExperienceOrbAccessor) orb).brainagelib$repairPlayerItems(player, amount);
        if (remaining > 0) {
            player.giveExperiencePoints(remaining);
        }
        return true;
    }

    private static @Nullable ServerPlayer activePlayer(ServerLevel level) {
        ArrayList<@Nullable ServerPlayer> recipients = RECIPIENTS.get();
        if (recipients == null) {
            return null;
        }

        // A dead player still resolves as a killer or TNT owner until they respawn, and a corpse's
        // inventory is
        // discarded at respawn: their drops stay in the world instead.
        ServerPlayer player = recipients.getLast();
        if (player == null
                || !player.isAlive()
                || player.level() != level
                || !DropPolicies.delivers(player)) {
            return null;
        }
        return player;
    }
}
