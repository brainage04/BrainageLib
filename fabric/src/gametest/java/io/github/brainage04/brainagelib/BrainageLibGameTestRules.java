package io.github.brainage04.brainagelib;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Registers the GameTest rules the way DEVELOPMENT.md tells a Fabric mod to. */
public final class BrainageLibGameTestRules implements ModInitializer {
    @Override
    public void onInitialize() {
        GameRuleSetGameTests.RULES.registerAll(
                (id, rule) -> Registry.register(BuiltInRegistries.GAME_RULE, id, rule));
    }
}
