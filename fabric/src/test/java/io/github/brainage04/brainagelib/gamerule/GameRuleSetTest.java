package io.github.brainage04.brainagelib.gamerule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GameRuleSetTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void rulesGetIdsInTheNamespaceAndKeepTheOrderTheyWereAddedIn() {
        GameRuleSet set = GameRuleSet.create("example");
        GameRule<Boolean> flag = set.bool("flag", GameRuleCategory.PLAYER, true);
        GameRule<Integer> count = set.integer("count", GameRuleCategory.MISC, 2, 0);
        GameRule<Integer> percent = set.integer("percent", GameRuleCategory.DROPS, 50, 0, 100);
        GameRule<Boolean> built = GameRuleFactory.bool(GameRuleCategory.MOBS, false);
        assertSame(built, set.add("built/elsewhere", built));

        assertEquals(
                List.of(id("flag"), id("count"), id("percent"), id("built/elsewhere")),
                List.copyOf(set.rules().keySet()));
        assertEquals(List.of(flag, count, percent, built), List.copyOf(set.rules().values()));
        assertEquals(true, flag.defaultValue());
        assertEquals(GameRuleCategory.PLAYER, flag.category());
        assertEquals(2, count.defaultValue());
        assertTrue(count.deserialize("-1").isError());
        assertEquals(100, percent.deserialize("100").getOrThrow());
        assertTrue(percent.deserialize("101").isError());
        assertEquals(GameRuleCategory.DROPS, percent.category());
    }

    @Test
    void rulesViewIsASnapshotTheCallerCannotChange() {
        GameRuleSet set = GameRuleSet.create("example");
        set.bool("flag", GameRuleCategory.PLAYER, true);
        Map<Identifier, GameRule<?>> rules = set.rules();
        assertThrows(UnsupportedOperationException.class, rules::clear);
        set.bool("later", GameRuleCategory.PLAYER, true);
        assertEquals(1, rules.size());
        assertEquals(2, set.rules().size());
    }

    @Test
    void registerAllPutsEveryRuleInARegistryUnderItsId() {
        GameRuleSet set = GameRuleSet.create("example");
        GameRule<Boolean> flag = set.bool("flag", GameRuleCategory.PLAYER, false);
        GameRule<Integer> count = set.integer("count", GameRuleCategory.MISC, 4, 1, 8);
        MappedRegistry<GameRule<?>> registry =
                new MappedRegistry<>(Registries.GAME_RULE, Lifecycle.stable());

        // The call a Fabric mod makes with BuiltInRegistries.GAME_RULE.
        set.registerAll((id, rule) -> Registry.register(registry, id, rule));

        assertEquals(id("flag"), registry.getKey(flag));
        assertEquals(id("count"), registry.getKey(count));
        assertSame(count, registry.getValue(id("count")));
        assertEquals(2, registry.size());

        GameRules values = new GameRules(List.of(flag, count));
        assertEquals(false, values.get(flag));
        assertEquals(4, values.get(count));
        values.set(count, 8, null);
        assertEquals(8, values.get(count));
    }

    @Test
    void registrationHappensOnceAndLateRulesFailLoudly() {
        GameRuleSet set = GameRuleSet.create("example");
        set.bool("flag", GameRuleCategory.PLAYER, true);
        List<Identifier> registered = new ArrayList<>();
        set.registerAll((id, rule) -> registered.add(id));
        assertEquals(List.of(id("flag")), registered);

        assertThrows(
                IllegalStateException.class, () -> set.bool("late", GameRuleCategory.PLAYER, true));
        assertThrows(IllegalStateException.class, () -> set.registerAll((id, rule) -> {}));
        assertEquals(List.of(id("flag")), List.copyOf(set.rules().keySet()));
    }

    @Test
    void anEmptySetRegistersNothing() {
        List<Identifier> registered = new ArrayList<>();
        GameRuleSet.create("example").registerAll((id, rule) -> registered.add(id));
        assertTrue(registered.isEmpty());
    }

    @Test
    void invalidIdsAndDuplicatesAreRefusedWhenAdded() {
        assertThrows(IllegalArgumentException.class, () -> GameRuleSet.create("Example"));
        assertThrows(IllegalArgumentException.class, () -> GameRuleSet.create("ex:ample"));
        assertThrows(NullPointerException.class, () -> GameRuleSet.create(null));

        GameRuleSet set = GameRuleSet.create("example");
        GameRule<Boolean> flag = set.bool("flag", GameRuleCategory.PLAYER, true);
        assertThrows(
                IllegalArgumentException.class,
                () -> set.bool("Flag", GameRuleCategory.PLAYER, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> set.bool("flag", GameRuleCategory.MISC, false));
        assertThrows(IllegalArgumentException.class, () -> set.add("other", flag));
        assertThrows(
                IllegalArgumentException.class,
                () -> set.integer("count", GameRuleCategory.MISC, 9, 0, 8));
        assertThrows(NullPointerException.class, () -> set.add("other", null));
        assertThrows(NullPointerException.class, () -> set.registerAll(null));
        assertEquals(List.of(id("flag")), List.copyOf(set.rules().keySet()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("example", path);
    }
}
