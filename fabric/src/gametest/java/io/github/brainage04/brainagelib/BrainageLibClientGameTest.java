package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.brainagelib.server.CurrentServer;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import java.lang.reflect.Field;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.server.players.NameAndId;

@SuppressWarnings("UnstableApiUsage")
public final class BrainageLibClientGameTest implements FabricClientGameTest {
    private static final ServerModHelpEntry PLAYER_HELP =
            ServerModHelpEntry.playerOnly(
                    "brainagelib_client_player",
                    "Client Player Tools",
                    "Client player commands.",
                    "/clientplayer help");
    private static final ServerModHelpEntry OPERATOR_CONFIG =
            new ServerModHelpEntry(
                    "brainagelib_client_operator",
                    "Client Operator Tools",
                    "Client operator commands.",
                    "/clientoperator help",
                    "/clientoperator config");

    private static final int STEP_VISIBLE_TICKS = 50;

    @Override
    public void runTest(ClientGameTestContext context) {
        ServerModHelpRegistry.register(PLAYER_HELP);
        ServerModHelpRegistry.register(OPERATOR_CONFIG);
        try {
            ClientGameTestServers.withDedicatedServer(
                    context,
                    ClientGameTestServers.flatServerProperties(),
                    "BrainageLib client chat behavior",
                    server -> {
                        awaitChat(
                                context,
                                "This server uses",
                                "Client Player Tools",
                                "Client Operator Tools");
                        server.runOnServer(
                                minecraftServer -> {
                                    var player =
                                            minecraftServer.getPlayerList().getPlayers().getFirst();
                                    FirstJoinHelpState.playerJoined(player, minecraftServer);
                                    FirstJoinHelpState.playerJoined(player, minecraftServer);
                                });
                        // Synchronize a later response before checking that no duplicate notice
                        // arrived.
                        sendCommand(context, "servermods help");
                        awaitChat(
                                context,
                                "Client Player Tools: Client player commands. /clientplayer help",
                                "Client Operator Tools: Client operator commands. /clientoperator help");
                        long notices =
                                chat(context).stream()
                                        .filter(line -> line.contains("This server uses"))
                                        .count();
                        if (notices != 1)
                            throw new AssertionError(
                                    "Expected exactly one first-join notice, got " + notices);
                        showStep(
                                context,
                                "brainagelib.help",
                                "One notice, one help list",
                                "The first-join notice names every server mod once; /servermods help lists them");

                        clearChat(context);
                        sendCommand(context, "servermods config");
                        awaitChat(context, "Incorrect argument for command", "config<--[HERE]");
                        if (chat(context).stream()
                                .anyMatch(line -> line.contains("/clientoperator config"))) {
                            throw new AssertionError(
                                    "Non-operator received configuration commands");
                        }
                        showStep(
                                context,
                                "brainagelib.config.denied",
                                "Configuration needs operator",
                                "/servermods config is refused for a player without operator permission");

                        server.runOnServer(
                                minecraftServer ->
                                        minecraftServer
                                                .getPlayerList()
                                                .op(
                                                        new NameAndId(
                                                                minecraftServer
                                                                        .getPlayerList()
                                                                        .getPlayers()
                                                                        .getFirst()
                                                                        .getGameProfile())));
                        clearChat(context);
                        sendCommand(context, "servermods config");
                        awaitChat(context, "Client Operator Tools: /clientoperator config");
                        if (chat(context).stream()
                                .anyMatch(line -> line.contains("Client Player Tools"))) {
                            throw new AssertionError(
                                    "Player-only entry leaked into configuration listing");
                        }
                        showStep(
                                context,
                                "brainagelib.config",
                                "Operator configuration commands",
                                "/servermods config lists only the mods that have configuration commands");

                        clearChat(context);
                        server.runOnServer(
                                minecraftServer -> {
                                    var player =
                                            minecraftServer.getPlayerList().getPlayers().getFirst();
                                    var feedback = ModFeedback.create("Client Test");
                                    feedback.neutral(player, "Mode: %s", "safe");
                                    feedback.success(player, "Settings saved");
                                    feedback.error(player, "Settings rejected");
                                });
                        awaitChat(
                                context,
                                "[Client Test] Mode: safe",
                                "[Client Test] Settings saved",
                                "[Client Test] Settings rejected");
                        showStep(
                                context,
                                "brainagelib.feedback",
                                "Consistent feedback",
                                "ModFeedback adds the grey mod-name prefix and colours neutral, success and error messages");

                        if (!server.computeOnServer(
                                minecraftServer -> CurrentServer.get() == minecraftServer))
                            throw new AssertionError("The running dedicated server is not current");
                    });
            // The test's dedicated server runs in this game instance, so its end must clear it.
            context.waitFor(client -> CurrentServer.get() == null);
        } finally {
            ServerModHelpRegistry.unregister(OPERATOR_CONFIG);
            ServerModHelpRegistry.unregister(PLAYER_HELP);
        }
    }

    private static void showStep(
            ClientGameTestContext context, String id, String title, String description) {
        ClientGameTestRecorder.showStep(context, id, title, description);
        context.waitTicks(STEP_VISIBLE_TICKS);
    }

    private static void sendCommand(ClientGameTestContext context, String command) {
        context.runOnClient(
                client -> {
                    if (client.getConnection() == null)
                        throw new AssertionError("Client is not connected");
                    client.getConnection().sendCommand(command);
                });
    }

    private static void clearChat(ClientGameTestContext context) {
        context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
    }

    private static void awaitChat(ClientGameTestContext context, String... expected) {
        for (int tick = 0; tick < 200; tick++) {
            List<String> messages = chat(context);
            if (List.of(expected).stream()
                    .allMatch(text -> messages.stream().anyMatch(line -> line.contains(text))))
                return;
            context.waitTicks(1);
        }
        throw new AssertionError(
                "Missing chat " + List.of(expected) + "; received " + chat(context));
    }

    private static List<String> chat(ClientGameTestContext context) {
        return context.computeOnClient(
                client -> {
                    try {
                        Field field = ChatComponent.class.getDeclaredField("allMessages");
                        field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<GuiMessage> messages =
                                (List<GuiMessage>) field.get(client.gui.hud.getChat());
                        return messages.stream()
                                .map(message -> message.content().getString())
                                .toList();
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Cannot inspect actual client chat", exception);
                    }
                });
    }
}
