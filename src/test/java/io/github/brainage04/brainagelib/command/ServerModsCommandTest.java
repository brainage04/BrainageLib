package io.github.brainage04.brainagelib.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerModsCommandTest {
    @Test
    void initializeRegistersServerModsCommands() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        ServerModsCommand.initialize(dispatcher);

        var root = dispatcher.getRoot().getChild(ServerModsCommand.COMMAND_NAME);
        assertNotNull(root);
        assertNotNull(root.getChild("help"));
        assertNotNull(root.getChild("config"));
    }
}
