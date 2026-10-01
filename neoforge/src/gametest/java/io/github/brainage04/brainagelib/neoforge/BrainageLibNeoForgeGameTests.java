package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.BrainageLibGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the shared GameTests as NeoForge test functions. Each function needs a matching
 * {@code data/<mod_id>/test_instance/<name>.json} in this source set's resources.
 */
@EventBusSubscriber(modid = BrainageLib.MOD_ID)
public final class BrainageLibNeoForgeGameTests {
    private BrainageLibNeoForgeGameTests() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "servermods_command_is_registered"),
                () -> BrainageLibGameTests::serverModsCommandIsRegistered
        );
    }
}
