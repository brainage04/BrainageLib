package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.command.ServerModsCommand;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(BrainageLib.MOD_ID)
public final class BrainageLibNeoForge {
    public BrainageLibNeoForge(IEventBus modEventBus) {
        BrainageLib.initialize();
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
        NeoForge.EVENT_BUS.addListener(this::playerLoggedIn);
    }
    private void registerCommands(RegisterCommandsEvent event) { ServerModsCommand.initialize(event.getDispatcher()); }
    private void serverStarted(ServerStartedEvent event) { FirstJoinHelpState.serverStarted(event.getServer()); }
    private void serverStopping(ServerStoppingEvent event) { FirstJoinHelpState.serverStopping(event.getServer()); }
    private void serverStopped(ServerStoppedEvent event) { FirstJoinHelpState.serverStopped(); }
    private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) FirstJoinHelpState.playerJoined(player, player.level().getServer());
    }
}
