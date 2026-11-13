package io.github.brainage04.brainagelib.gamerule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Compares the built rules with rules the game registers itself through its private helpers. */
class GameRuleFactoryTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void booleanRuleIsBuiltLikeTheGamesOwnBooleanRules() {
        // keep_inventory: GameRules.registerBoolean("keep_inventory", PLAYER, false).
        GameRule<?> vanilla = GameRules.KEEP_INVENTORY;
        GameRule<Boolean> rule = GameRuleFactory.bool(GameRuleCategory.PLAYER, false);

        assertSameShape(vanilla, rule);
        assertSame(vanilla.valueCodec(), rule.valueCodec());
        assertEquals(false, rule.defaultValue());
        assertEquals(Boolean.class, rule.valueClass());
        assertEquals(1, rule.getCommandResult(true));
        assertEquals(0, rule.getCommandResult(false));
        assertEquals(true, rule.deserialize("true").getOrThrow());
        assertTrue(rule.deserialize("yes").isError());
        assertTrue(GameRules.KEEP_INVENTORY.deserialize("yes").isError());
        assertEquals(List.of("visit", "boolean"), visits(rule));
        assertEquals(visits(vanilla), visits(rule));
        assertEquals(true, GameRuleFactory.bool(GameRuleCategory.MISC, true).defaultValue());
    }

    @Test
    void integerRuleWithOnlyAMinimumIsBuiltLikeTheGamesOwn() {
        // max_entity_cramming: GameRules.registerInteger("max_entity_cramming", MOBS, 24, 0).
        GameRule<?> vanilla = GameRules.MAX_ENTITY_CRAMMING;
        GameRule<Integer> rule = GameRuleFactory.integer(GameRuleCategory.MOBS, 24, 0);

        assertSameShape(vanilla, rule);
        assertEquals(24, rule.defaultValue());
        assertEquals(Integer.class, rule.valueClass());
        assertEquals(24, rule.getCommandResult(24));
        assertEquals(List.of("visit", "integer"), visits(rule));
        assertEquals(visits(vanilla), visits(rule));
        assertSameValues(vanilla, rule);
        assertTrue(rule.deserialize("-1").isError());
        assertEquals(
                Integer.MAX_VALUE,
                rule.deserialize(Integer.toString(Integer.MAX_VALUE)).getOrThrow());
    }

    @Test
    void integerRuleWithARangeIsBuiltLikeTheGamesOwn() {
        // max_snow_accumulation_height: GameRules.registerInteger(..., UPDATES, 1, 0, 8).
        GameRule<?> vanilla = GameRules.MAX_SNOW_ACCUMULATION_HEIGHT;
        GameRule<Integer> rule = GameRuleFactory.integer(GameRuleCategory.UPDATES, 1, 0, 8);

        assertSameShape(vanilla, rule);
        assertSameValues(vanilla, rule);
        assertEquals(8, rule.deserialize("8").getOrThrow());
        assertTrue(rule.deserialize("9").isError());
        assertTrue(rule.valueCodec().parse(JsonOps.INSTANCE, new JsonPrimitive(9)).isError());
        assertEquals(
                new JsonPrimitive(8),
                rule.valueCodec().encodeStart(JsonOps.INSTANCE, 8).getOrThrow());
    }

    @Test
    void integerRuleAcceptsTheWholeIntRangeAndSingleValueRanges() {
        GameRule<Integer> any =
                GameRuleFactory.integer(
                        GameRuleCategory.MISC, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(
                Integer.MIN_VALUE,
                any.deserialize(Integer.toString(Integer.MIN_VALUE)).getOrThrow());
        GameRule<Integer> fixed = GameRuleFactory.integer(GameRuleCategory.MISC, 3, 3, 3);
        assertEquals(3, fixed.deserialize("3").getOrThrow());
        assertTrue(fixed.deserialize("4").isError());
    }

    @Test
    void impossibleRangesAndDefaultsAreRefusedWhenTheRuleIsBuilt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GameRuleFactory.integer(GameRuleCategory.MISC, 5, 6, 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> GameRuleFactory.integer(GameRuleCategory.MISC, -1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> GameRuleFactory.integer(GameRuleCategory.MISC, 9, 0, 8));
        assertThrows(NullPointerException.class, () -> GameRuleFactory.bool(null, true));
        assertThrows(NullPointerException.class, () -> GameRuleFactory.integer(null, 0, 0));
    }

    private static void assertSameShape(GameRule<?> vanilla, GameRule<?> rule) {
        assertEquals(vanilla.category(), rule.category());
        assertEquals(vanilla.gameRuleType(), rule.gameRuleType());
        // BoolArgumentType has no equals; IntegerArgumentType's equals compares the range.
        assertEquals(vanilla.argument().getClass(), rule.argument().getClass());
        if (rule.argument() instanceof IntegerArgumentType) {
            assertEquals(vanilla.argument(), rule.argument());
        }
        assertEquals(vanilla.requiredFeatures(), rule.requiredFeatures());
        assertEquals(vanilla.valueClass(), rule.valueClass());
    }

    /** Both rules accept and reject the same saved values around the vanilla rule's range. */
    @SuppressWarnings("unchecked")
    private static void assertSameValues(GameRule<?> vanilla, GameRule<Integer> rule) {
        GameRule<Integer> vanillaInteger = (GameRule<Integer>) vanilla;
        for (int candidate : new int[] {Integer.MIN_VALUE, -1, 0, 1, 8, 9, 24, Integer.MAX_VALUE}) {
            JsonPrimitive json = new JsonPrimitive(candidate);
            assertEquals(
                    vanillaInteger.valueCodec().parse(JsonOps.INSTANCE, json).result(),
                    rule.valueCodec().parse(JsonOps.INSTANCE, json).result(),
                    "saved value " + candidate);
            assertEquals(
                    vanillaInteger.deserialize(Integer.toString(candidate)).result(),
                    rule.deserialize(Integer.toString(candidate)).result(),
                    "command value " + candidate);
        }
    }

    /** The visitor methods the edit-game-rules screen and the gamerule command would see. */
    private static List<String> visits(GameRule<?> rule) {
        List<String> seen = new ArrayList<>();
        GameRuleTypeVisitor visitor =
                new GameRuleTypeVisitor() {
                    @Override
                    public <T> void visit(GameRule<T> gameRule) {
                        seen.add("visit");
                    }

                    @Override
                    public void visitBoolean(GameRule<Boolean> gameRule) {
                        seen.add("boolean");
                    }

                    @Override
                    public void visitInteger(GameRule<Integer> gameRule) {
                        seen.add("integer");
                    }
                };
        // As GameRules.visitGameRuleTypes does (GameRules.java:160-165 in 26.3): visit, then the
        // rule's own caller.
        visitor.visit(rule);
        rule.callVisitor(visitor);
        return seen;
    }
}
