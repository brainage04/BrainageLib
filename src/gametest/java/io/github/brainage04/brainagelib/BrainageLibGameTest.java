package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.command.ServerModsCommand;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class BrainageLibGameTest {
    @GameTest
    public void serverModsCommandIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(ServerModsCommand.COMMAND_NAME) == null) {
            throw new AssertionError("Expected the servermods command to be registered on the dedicated server.");
        }

        context.succeed();
    }
}
