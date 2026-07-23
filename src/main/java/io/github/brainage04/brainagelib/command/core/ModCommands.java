package io.github.brainage04.brainagelib.command.core;

import io.github.brainage04.brainagelib.command.ServerModsCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class ModCommands {
    private ModCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ServerModsCommand.initialize(dispatcher));
    }
}
