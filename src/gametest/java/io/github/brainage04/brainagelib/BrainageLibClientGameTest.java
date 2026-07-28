package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.command.ServerModsCommand;
import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.List;
import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class BrainageLibClientGameTest implements FabricClientGameTest {
    private static final ServerModHelpEntry PLAYER_HELP = ServerModHelpEntry.playerOnly(
            "brainagelib-recording-player",
            "Recording Player Tools",
            "Deterministic player commands for this server.",
            "/recordingplayer help"
    );
    private static final ServerModHelpEntry OPERATOR_CONFIG = new ServerModHelpEntry(
            "brainagelib-recording-config",
            "Recording Operator Tools",
            "Deterministic operator configuration for this server.",
            "/recordingoperator help",
            "/recordingoperator config"
    );

    @Override
    public void runTest(ClientGameTestContext context) {
        registerFixtureEntries();
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "BrainageLib servermods feedback recording GameTest", server -> { try {
            server.runOnServer(minecraftServer -> prepareAndAssertServer(
                    minecraftServer,
                    minecraftServer.getPlayerList().getPlayers().getFirst()));
            ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
            context.waitTicks(20);
            assertClientCommandTree(context);
            context.runOnClient(client -> client.setScreenAndShow(new ChatScreen("", false)));
            ClientGameTestRecorder.startRecording(context);
        
            runCommand(context, "servermods help");
            context.waitTicks(20);
            ClientGameTestRecorder.showStep(
                    context,
                    "servermods-help",
                    "Installed server-mod help",
                    "/servermods help lists the player-facing help commands for both deterministic fixture mods."
            );
            context.waitTicks(60);
        
            runCommand(context, "servermods config");
            context.waitTicks(20);
            ClientGameTestRecorder.showStep(
                    context,
                    "servermods-config",
                    "Operator configuration commands",
                    "/servermods config lists the fixture's operator configuration command alongside the library's neutral and success-styled feedback."
            );
            context.waitTicks(60);
        
            runCommand(context, "servermods unavailable");
            context.waitTicks(20);
            ClientGameTestRecorder.showStep(
                    context,
                    "servermods-invalid-command",
                    "Invalid subcommand feedback",
                    "The command's real Brigadier failure feedback is visible in chat after the help and configuration listings."
            );
            context.waitTicks(60);
        } finally {
            ServerModHelpRegistry.unregister(OPERATOR_CONFIG);
            ServerModHelpRegistry.unregister(PLAYER_HELP);
            context.runOnClient(client -> client.setScreenAndShow(null));
            ;
        } });
    }

    private static void registerFixtureEntries() {
        ServerModHelpRegistry.register(PLAYER_HELP);
        ServerModHelpRegistry.register(OPERATOR_CONFIG);
    }

    private static void prepareAndAssertServer(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
        server.getPlayerList().op(new NameAndId(player.getGameProfile()));
        if (server.getCommands().getDispatcher().getRoot().getChild(ServerModsCommand.COMMAND_NAME) == null) {
            throw new AssertionError("Expected the dedicated server to register /servermods.");
        }
        if (!ServerModHelpRegistry.entries().containsAll(List.of(PLAYER_HELP, OPERATOR_CONFIG))) {
            throw new AssertionError("Expected the deterministic help and configuration fixture entries to remain registered.");
        }
    }

    private static void assertClientCommandTree(ClientGameTestContext context) {
        context.computeOnClient(client -> {
            if (client.getConnection() == null) {
                throw new AssertionError("Expected a connected client for /servermods command verification.");
            }
            var serverMods = client.getConnection().getCommands().getRoot().getChild(ServerModsCommand.COMMAND_NAME);
            if (serverMods == null || serverMods.getChild("help") == null || serverMods.getChild("config") == null) {
                throw new AssertionError("Expected /servermods with help and config roots to be synchronized to the client.");
            }
            return null;
        });
    }

    private static void runCommand(ClientGameTestContext context, String command) {
        context.runOnClient(client -> {
            if (client.getConnection() == null) {
                throw new AssertionError("Expected a connected client to run /" + command + '.');
            }
            client.getConnection().sendCommand(command);
        });
    }
}
