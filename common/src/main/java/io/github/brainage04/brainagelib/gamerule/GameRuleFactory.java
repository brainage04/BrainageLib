package io.github.brainage04.brainagelib.gamerule;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;

/**
 * Builds boolean and integer game rules the way the game builds its own, so that {@code /gamerule},
 * the world's saved rules and the edit-game-rules screen treat a mod's rule like a vanilla one.
 *
 * <p>The rules are not registered here. Give them ids and register them with {@link GameRuleSet},
 * or register them yourself.
 */
public final class GameRuleFactory {
    private GameRuleFactory() {}

    // The game's own helpers are private (GameRules.registerBoolean and registerInteger,
    // GameRules.java:167-203 in 26.3), so these repeat the arguments they pass to the GameRule
    // constructor.

    /** A true/false rule; {@code /gamerule} reports 1 for true and 0 for false. */
    public static GameRule<Boolean> bool(GameRuleCategory category, boolean defaultValue) {
        return new GameRule<>(
                Objects.requireNonNull(category, "category"),
                GameRuleType.BOOL,
                BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean,
                Codec.BOOL,
                value -> value ? 1 : 0,
                defaultValue,
                FeatureFlagSet.of());
    }

    /** A whole-number rule from {@code min} up to {@link Integer#MAX_VALUE}. */
    public static GameRule<Integer> integer(GameRuleCategory category, int defaultValue, int min) {
        return integer(category, defaultValue, min, Integer.MAX_VALUE);
    }

    /**
     * A whole-number rule from {@code min} to {@code max}, both included; {@code /gamerule} reports
     * the value itself.
     *
     * @throws IllegalArgumentException if {@code min} is above {@code max} or the default is
     *     outside the range
     */
    public static GameRule<Integer> integer(
            GameRuleCategory category, int defaultValue, int min, int max) {
        Objects.requireNonNull(category, "category");
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " is above max " + max);
        }
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException(
                    "default " + defaultValue + " is outside " + min + ".." + max);
        }
        return new GameRule<>(
                category,
                GameRuleType.INT,
                IntegerArgumentType.integer(min, max),
                GameRuleTypeVisitor::visitInteger,
                Codec.intRange(min, max),
                value -> value,
                defaultValue,
                FeatureFlagSet.of());
    }
}
