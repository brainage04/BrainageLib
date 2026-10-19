package io.github.brainage04.brainagelib;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The same behavioral bodies run against both loaders' real command dispatchers. */
public final class BrainageLibGameTests {
    private BrainageLibGameTests() {}

    public static void helpCommandsExecuteAndRestrictConfiguration(GameTestHelper context) {
        var server = context.getLevel().getServer();
        var playerOnly = ServerModHelpEntry.playerOnly("gametest_player", "Alpha Test Tools", "Player tools.", "/alpha help");
        var configurable = new ServerModHelpEntry("gametest_operator", "Zeta Test Tools", "Operator tools.", "/zeta help", "/zeta config");
        ServerModHelpRegistry.register(configurable);
        ServerModHelpRegistry.register(playerOnly);
        try {
            var output = new CapturedOutput();
            var source = server.createCommandSourceStack().withSource(output).withPermission(PermissionSet.NO_PERMISSIONS);
            var dispatcher = server.getCommands().getDispatcher();
            context.assertValueEqual(dispatcher.execute("servermods", source), 1, "Root command result");
            var rootMessages = output.text();
            context.assertTrue(rootMessages.contains("Alpha Test Tools: Player tools. /alpha help"), "Player help must include description and command");
            context.assertTrue(rootMessages.indexOf("Alpha Test Tools") < rootMessages.indexOf("Zeta Test Tools"), "Help must be sorted by display name");
            output.messages.clear();
            dispatcher.execute("servermods help", source);
            context.assertValueEqual(output.text(), rootMessages, "Root and help must produce the same output");
            output.messages.clear();
            try {
                dispatcher.execute("servermods config", source);
                context.fail("Non-operators must not execute the config command");
            } catch (CommandSyntaxException expected) {
                context.assertTrue(output.messages.isEmpty(), "Denied config must not leak command listings");
            }
            dispatcher.execute("servermods config", source.withPermission(PermissionSet.ALL_PERMISSIONS));
            context.assertTrue(output.text().contains("Zeta Test Tools: /zeta config"), "Operators must see configuration commands");
            context.assertFalse(output.text().contains("Alpha Test Tools"), "Player-only mods must not appear in config output");
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Expected a valid servermods command", exception);
        } finally {
            ServerModHelpRegistry.unregister(configurable);
            ServerModHelpRegistry.unregister(playerOnly);
        }
        context.succeed();
    }

    public static void feedbackDeliversFormattedMessagesAndResults(GameTestHelper context) {
        var output = new CapturedOutput();
        var source = context.getLevel().getServer().createCommandSourceStack().withSource(output);
        var feedback = ModFeedback.create("Test Tools");
        context.assertValueEqual(feedback.neutral(source, "Mode: %s", "safe"), 1, "Neutral result");
        context.assertValueEqual(feedback.success(source, "Saved %s", false, "settings"), 1, "Success result");
        context.assertValueEqual(feedback.failure(source, "Cannot save %s", "settings"), 0, "Failure result");
        context.assertValueEqual(output.messages.size(), 3, "Each feedback call must send one message");
        String[] text = {"[Test Tools] Mode: safe", "[Test Tools] Saved settings", "[Test Tools] Cannot save settings"};
        ChatFormatting[] colors = {ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.RED};
        for (int index = 0; index < text.length; index++) {
            Component message = output.messages.get(index);
            context.assertValueEqual(message.getString(), text[index], "Formatted feedback text");
            context.assertValueEqual(message.getStyle().getColor().getValue(), ChatFormatting.GRAY.getColor(), "Prefix color");
            context.assertValueEqual(message.getSiblings().getFirst().getStyle().getColor().getValue(), colors[index].getColor(), "Feedback body color");
        }
        context.succeed();
    }

    public static void firstJoinAcknowledgementSurvivesReload(GameTestHelper context) {
        var server = context.getLevel().getServer();
        var path = server.getWorldPath(LevelResource.ROOT).resolve("brainagelib-first-join-help.json");
        var entry = ServerModHelpEntry.playerOnly("gametest_join", "Join Test Tools", "Join tools.", "/join help");
        byte[] original;
        try {
            original = Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException exception) {
            throw new AssertionError("Cannot preserve first-join state", exception);
        }
        ServerModHelpRegistry.register(entry);
        try {
            Files.deleteIfExists(path);
            FirstJoinHelpState.serverStopped();
            FirstJoinHelpState.serverStarted(server);
            var player = new CapturedPlayer(context);
            FirstJoinHelpState.playerJoined(player, server);
            FirstJoinHelpState.playerJoined(player, server);
            context.assertValueEqual(player.messages.size(), 1, "First join must send exactly one notice");
            context.assertTrue(player.messages.getFirst().getString().contains("Join Test Tools"), "The combined notice must name registered mods");
            context.assertTrue(Files.readString(path).contains(player.getUUID().toString()), "Acknowledgement must be written immediately");
            FirstJoinHelpState.serverStopping(server);
            FirstJoinHelpState.serverStopped();
            FirstJoinHelpState.serverStarted(server);
            FirstJoinHelpState.playerJoined(player, server);
            context.assertValueEqual(player.messages.size(), 1, "Reload must not repeat an acknowledged notice");
            Files.delete(path);
            FirstJoinHelpState.serverStopped();
            FirstJoinHelpState.serverStarted(server);
            FirstJoinHelpState.playerJoined(player, server);
            context.assertValueEqual(player.messages.size(), 2, "A fresh world's state must not retain previous acknowledgements");
        } catch (IOException exception) {
            throw new AssertionError("First-join persistence failed", exception);
        } finally {
            ServerModHelpRegistry.unregister(entry);
            try {
                if (original == null) Files.deleteIfExists(path);
                else Files.write(path, original);
            } catch (IOException exception) {
                throw new AssertionError("Cannot restore first-join state", exception);
            } finally {
                FirstJoinHelpState.serverStopped();
                FirstJoinHelpState.serverStarted(server);
            }
        }
        context.succeed();
    }

    private static final class CapturedOutput implements CommandSource {
        private final List<Component> messages = new ArrayList<>();
        @Override public void sendSystemMessage(Component message) { messages.add(message); }
        @Override public boolean acceptsSuccess() { return true; }
        @Override public boolean acceptsFailure() { return true; }
        @Override public boolean shouldInformAdmins() { return false; }
        private String text() { return String.join("\n", messages.stream().map(Component::getString).toList()); }
    }

    private static final class CapturedPlayer extends ServerPlayer {
        private final List<Component> messages = new ArrayList<>();
        private CapturedPlayer(GameTestHelper context) {
            super(context.getLevel().getServer(), context.getLevel(),
                    new GameProfile(UUID.fromString("f31ea7e0-04ba-4ed1-aaad-e37bdfc71560"), "JoinTest"), ClientInformation.createDefault());
        }
        @Override public void sendSystemMessage(Component message) { messages.add(message); }
    }
}
