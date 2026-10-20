package io.github.brainage04.brainagelib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.brainagelib.gametest.CapturedCommandOutput;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
            var output = new CapturedCommandOutput();
            var source = output.source(server).withPermission(PermissionSet.NO_PERMISSIONS);
            var dispatcher = server.getCommands().getDispatcher();
            context.assertValueEqual(dispatcher.execute("servermods", source), 1, "Root command result");
            var rootMessages = output.text();
            context.assertTrue(rootMessages.contains("Alpha Test Tools: Player tools. /alpha help"), "Player help must include description and command");
            context.assertTrue(rootMessages.indexOf("Alpha Test Tools") < rootMessages.indexOf("Zeta Test Tools"), "Help must be sorted by display name");
            output.clear();
            dispatcher.execute("servermods help", source);
            context.assertValueEqual(output.text(), rootMessages, "Root and help must produce the same output");
            output.clear();
            try {
                dispatcher.execute("servermods config", source);
                context.fail("Non-operators must not execute the config command");
            } catch (CommandSyntaxException expected) {
                context.assertTrue(output.messages().isEmpty(), "Denied config must not leak command listings");
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
        var output = new CapturedCommandOutput();
        var source = output.source(context.getLevel().getServer());
        var feedback = ModFeedback.create("Test Tools");
        context.assertValueEqual(feedback.neutral(source, "Mode: %s", "safe"), 1, "Neutral result");
        context.assertValueEqual(feedback.success(source, "Saved %s", false, "settings"), 1, "Success result");
        context.assertValueEqual(feedback.failure(source, "Cannot save %s", "settings"), 0, "Failure result");
        context.assertValueEqual(output.messages().size(), 3, "Each feedback call must send one message");
        assertFormatted(context, output.messages());
        try (var player = GameTestPlayer.join(context, "FeedbackTest")) {
            player.clearMessages();
            feedback.neutral(player.player(), "Mode: %s", "safe");
            feedback.success(player.player(), "Saved %s", "settings");
            feedback.error(player.player(), "Cannot save %s", "settings");
            assertFormatted(context, player.messages());
        }
        context.succeed();
    }

    /** The fixture joins a real player, delivers packets, runs commands and leaves nothing behind when closed. */
    public static void gameTestPlayerJoinsRunsCommandsAndCleansUp(GameTestHelper context) {
        var server = context.getLevel().getServer();
        var entry = ServerModHelpEntry.playerOnly("gametest_fixture", "Fixture Test Tools", "Fixture tools.", "/fixture help");
        ServerModHelpRegistry.register(entry);
        GameTestPlayer fixture = GameTestPlayer.join(context, "FixtureTest");
        ServerPlayer player = fixture.player();
        try {
            context.assertTrue(server.getPlayerList().getPlayer(player.getUUID()) == player, "The player must be on the server's player list");
            context.assertTrue(fixture.channel().isOpen(), "The connection's channel must be open");
            fixture.gameMode(GameType.SPECTATOR);
            context.assertTrue(player.gameMode() == GameType.SPECTATOR, "Game mode changes go through the server");
            fixture.clearMessages();
            fixture.command("servermods");
            context.assertTrue(fixture.messageTexts().stream().anyMatch(text -> text.contains("Fixture Test Tools: Fixture tools. /fixture help")),
                    "A command run through the chat-command packet must answer the player, got " + fixture.messageTexts());
        } finally {
            fixture.close();
            ServerModHelpRegistry.unregister(entry);
        }
        context.assertTrue(server.getPlayerList().getPlayer(player.getUUID()) == null, "Closing must remove the player");
        context.assertFalse(fixture.channel().isOpen(), "Closing must close the channel");
        context.assertFalse(fixture.connection().isConnected(), "Closing must disconnect the listener");
        fixture.close();
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
            // Joining fires the loader's join event, which sends the first notice.
            try (var fixture = GameTestPlayer.join(context, "JoinTest")) {
                var player = fixture.player();
                FirstJoinHelpState.playerJoined(player, server);
                context.assertValueEqual(notices(fixture, "Join Test Tools"), 1, "First join must send exactly one notice");
                context.assertTrue(Files.readString(path).contains(player.getUUID().toString()), "Acknowledgement must be written immediately");
                FirstJoinHelpState.serverStopping(server);
                FirstJoinHelpState.serverStopped();
                FirstJoinHelpState.serverStarted(server);
                FirstJoinHelpState.playerJoined(player, server);
                context.assertValueEqual(notices(fixture, "Join Test Tools"), 1, "Reload must not repeat an acknowledged notice");
                Files.delete(path);
                FirstJoinHelpState.serverStopped();
                FirstJoinHelpState.serverStarted(server);
                FirstJoinHelpState.playerJoined(player, server);
                context.assertValueEqual(notices(fixture, "Join Test Tools"), 2, "A fresh world's state must not retain previous acknowledgements");
            }
        } catch (IOException exception) {
            throw new AssertionError("First-join persistence failed", exception);
        } finally {
            ServerModHelpRegistry.unregister(entry);
            restore(server, path, original);
        }
        context.succeed();
    }

    /** A file written by BrainageLib 1.0.x keeps loading: its players are not notified again. */
    public static void firstJoinStateFromPreviousVersionLoads(GameTestHelper context) {
        var server = context.getLevel().getServer();
        var path = server.getWorldPath(LevelResource.ROOT).resolve("brainagelib-first-join-help.json");
        var entry = ServerModHelpEntry.playerOnly("gametest_legacy", "Legacy Test Tools", "Legacy tools.", "/legacy help");
        byte[] original;
        try {
            original = Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException exception) {
            throw new AssertionError("Cannot preserve first-join state", exception);
        }
        ServerModHelpRegistry.register(entry);
        try {
            FirstJoinHelpState.serverStopped();
            Files.deleteIfExists(path);
            try (var fixture = GameTestPlayer.join(context, "LegacyJoinTest")) {
                var id = fixture.player().getUUID().toString();
                // Byte for byte what 1.0.1 wrote: pretty-printed, sorted UUIDs, no trailing newline.
                String previous = "{\n  \"schemaVersion\": 1,\n  \"notifiedPlayers\": [\n    \"" + id + "\"\n  ]\n}";
                Files.writeString(path, previous);
                FirstJoinHelpState.serverStarted(server);
                fixture.clearMessages();
                FirstJoinHelpState.playerJoined(fixture.player(), server);
                context.assertValueEqual(notices(fixture, "Legacy Test Tools"), 0, "A player acknowledged by the previous version must not be notified again");
                FirstJoinHelpState.serverStopping(server);
                context.assertValueEqual(Files.readString(path), previous, "Saving unchanged state must write the same bytes");
            }
        } catch (IOException exception) {
            throw new AssertionError("First-join persistence failed", exception);
        } finally {
            ServerModHelpRegistry.unregister(entry);
            restore(server, path, original);
        }
        context.succeed();
    }

    /** A file that failed to load (malformed or a newer schema) must survive a later join and shutdown byte for byte. */
    public static void unreadableFirstJoinStateIsNotOverwritten(GameTestHelper context) {
        var server = context.getLevel().getServer();
        var path = server.getWorldPath(LevelResource.ROOT).resolve("brainagelib-first-join-help.json");
        var entry = ServerModHelpEntry.playerOnly("gametest_unreadable", "Unreadable Test Tools", "Unreadable tools.", "/unreadable help");
        byte[] original;
        try {
            original = Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException exception) {
            throw new AssertionError("Cannot preserve first-join state", exception);
        }
        ServerModHelpRegistry.register(entry);
        try {
            for (String document : List.of("{\"schemaVersion\": 1, \"notifiedPlayers\": [", "{\"schemaVersion\": 2, \"notifiedPlayers\": []}")) {
                byte[] bytes = document.getBytes(StandardCharsets.UTF_8);
                Files.write(path, bytes);
                FirstJoinHelpState.serverStopped();
                FirstJoinHelpState.serverStarted(server);
                try (var fixture = GameTestPlayer.join(context, "UnreadableTest")) {
                    FirstJoinHelpState.playerJoined(fixture.player(), server);
                    context.assertValueEqual(notices(fixture, "Unreadable Test Tools"), 1, "The notice still applies for the session: " + document);
                }
                FirstJoinHelpState.serverStopping(server);
                FirstJoinHelpState.serverStopped();
                context.assertTrue(Arrays.equals(Files.readAllBytes(path), bytes), "Unreadable state must survive a join and shutdown: " + document);
            }
        } catch (IOException exception) {
            throw new AssertionError("First-join persistence failed", exception);
        } finally {
            ServerModHelpRegistry.unregister(entry);
            restore(server, path, original);
        }
        context.succeed();
    }

    private static int notices(GameTestPlayer player, String modName) {
        return (int) player.messageTexts().stream().filter(text -> text.contains(modName)).count();
    }

    private static void restore(net.minecraft.server.MinecraftServer server, java.nio.file.Path path, byte[] original) {
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

    private static void assertFormatted(GameTestHelper context, List<Component> messages) {
        context.assertValueEqual(messages.size(), 3, "Each feedback call must send one message");
        String[] text = {"[Test Tools] Mode: safe", "[Test Tools] Saved settings", "[Test Tools] Cannot save settings"};
        int[] colors = {0xFFFF55, 0x55FF55, 0xFF5555};
        for (int index = 0; index < text.length; index++) {
            Component message = messages.get(index);
            context.assertValueEqual(message.getString(), text[index], "Formatted feedback text");
            List<Integer> visibleColors = new ArrayList<>();
            message.visit((style, segment) -> {
                if (!segment.isEmpty()) visibleColors.add(style.getColor().getValue());
                return Optional.empty();
            }, Style.EMPTY);
            context.assertValueEqual(visibleColors, List.of(0xAAAAAA, colors[index]), "Visible prefix and body colors");
        }
    }
}
