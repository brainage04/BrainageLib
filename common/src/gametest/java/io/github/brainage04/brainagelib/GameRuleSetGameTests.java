package io.github.brainage04.brainagelib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.gamerule.GameRuleSet;
import io.github.brainage04.brainagelib.gametest.CapturedCommandOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Rules from a {@link GameRuleSet}, registered by each loader's GameTest mod the way the class
 * documentation tells a mod to, behave like the game's own in a running server.
 */
public final class GameRuleSetGameTests {
    /** Registered by BrainageLibGameTestRules (Fabric) and BrainageLibNeoForgeGameTests. */
    public static final GameRuleSet RULES = GameRuleSet.create(BrainageLib.MOD_ID);

    private static final GameRule<Boolean> FLAG =
            RULES.bool("gametest_flag", GameRuleCategory.MISC, true);
    private static final GameRule<Integer> COUNT =
            RULES.integer("gametest_count", GameRuleCategory.MISC, 2, 1, 8);

    private GameRuleSetGameTests() {}

    public static void rulesWorkWithTheGameruleCommandAndAreSaved(GameTestHelper context) {
        var server = context.getLevel().getServer();
        context.assertValueEqual(
                BuiltInRegistries.GAME_RULE.getKey(FLAG),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "gametest_flag"),
                "The flag must be registered under its id");
        context.assertValueEqual(
                BuiltInRegistries.GAME_RULE.getKey(COUNT),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "gametest_count"),
                "The count must be registered under its id");

        GameRules rules = server.getGameRules();
        var output = new CapturedCommandOutput();
        var source = output.source(server);
        var dispatcher = server.getCommands().getDispatcher();
        try {
            context.assertValueEqual(rules.get(FLAG), true, "The flag starts at its default");
            context.assertValueEqual(rules.get(COUNT), 2, "The count starts at its default");
            context.assertValueEqual(
                    dispatcher.execute("gamerule brainagelib:gametest_flag", source),
                    1,
                    "Querying a true flag returns 1");
            context.assertValueEqual(
                    dispatcher.execute("gamerule brainagelib:gametest_flag false", source),
                    0,
                    "Setting the flag to false returns 0");
            context.assertValueEqual(rules.get(FLAG), false, "The command sets the flag");
            context.assertValueEqual(
                    dispatcher.execute("gamerule brainagelib:gametest_count 8", source),
                    8,
                    "Setting the count returns the value");
            context.assertValueEqual(rules.get(COUNT), 8, "The command sets the count");
            try {
                dispatcher.execute("gamerule brainagelib:gametest_count 9", source);
                context.fail("A count above the maximum must be refused");
            } catch (CommandSyntaxException expected) {
                context.assertValueEqual(rules.get(COUNT), 8, "A refused count changes nothing");
            }

            var codec = GameRules.codec(server.getWorldData().enabledFeatures());
            Tag saved = codec.encodeStart(NbtOps.INSTANCE, rules).getOrThrow();
            GameRules loaded = codec.parse(NbtOps.INSTANCE, saved).getOrThrow();
            context.assertValueEqual(loaded.get(FLAG), false, "The saved rules keep the flag");
            context.assertValueEqual(loaded.get(COUNT), 8, "The saved rules keep the count");
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Expected a valid gamerule command", exception);
        } finally {
            rules.set(FLAG, FLAG.defaultValue(), server);
            rules.set(COUNT, COUNT.defaultValue(), server);
        }
        context.succeed();
    }
}
