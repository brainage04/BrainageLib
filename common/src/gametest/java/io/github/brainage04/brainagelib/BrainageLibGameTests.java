package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.command.ServerModsCommand;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Loader-neutral server GameTest bodies. Both loaders compile this source set into their GameTest
 * mods: Fabric runs them through {@code @GameTest} methods, NeoForge through registered test
 * functions and {@code test_instance} data.
 */
public final class BrainageLibGameTests {
    private BrainageLibGameTests() {}

    public static void serverModsCommandIsRegistered(GameTestHelper context) {
        context.assertTrue(
                context.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(ServerModsCommand.COMMAND_NAME) != null,
                "Expected the servermods command to be registered on the dedicated server."
        );

        context.succeed();
    }
}
