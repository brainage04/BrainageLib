package io.github.brainage04.brainagelib.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static net.minecraft.commands.Commands.literal;

public final class ServerModsCommand {
    public static final String COMMAND_NAME = "servermods";

    private ServerModsCommand() {
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(COMMAND_NAME)
                .executes(context -> ServerModHelpRegistry.showHelp(context.getSource()))
                .then(literal("help")
                        .executes(context -> ServerModHelpRegistry.showHelp(context.getSource())))
                .then(literal("config")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> ServerModHelpRegistry.showAdminConfig(context.getSource()))));
    }
}
