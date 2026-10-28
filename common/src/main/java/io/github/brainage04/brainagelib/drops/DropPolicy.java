package io.github.brainage04.brainagelib.drops;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One mod's rules for which players get the drops {@link DropCapture} catches for them.
 *
 * <p>Register it with {@link DropPolicies#register}. For each player the highest-priority policy
 * that {@linkplain #governs governs} them decides alone, so a minigame can own its players during a
 * match while a general mod covers everyone else. A player no policy governs keeps every drop in
 * the world.
 */
public interface DropPolicy {
    /**
     * Whether this policy decides for {@code player} at all; when it does not, the next policy is
     * asked.
     */
    boolean governs(ServerPlayer player);

    /**
     * Whether the drops and experience caught for {@code player} go to them. Only asked when this
     * policy governs.
     */
    boolean delivers(ServerPlayer player);

    /**
     * Whether {@code stack}, caught for a player this policy delivers to, may go to them. A refused
     * stack stays where the game spawned it. Experience is never filtered.
     */
    default boolean accepts(ServerPlayer player, ItemStack stack) {
        return true;
    }
}
