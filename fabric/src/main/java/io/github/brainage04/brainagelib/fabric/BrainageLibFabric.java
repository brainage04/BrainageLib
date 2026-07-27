package io.github.brainage04.brainagelib.fabric;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.command.ServerModsCommand;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class BrainageLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BrainageLib.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ServerModsCommand.initialize(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(FirstJoinHelpState::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(FirstJoinHelpState::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FirstJoinHelpState.serverStopped());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> FirstJoinHelpState.playerJoined(handler.player, server));
    }
}
