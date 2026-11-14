package io.github.brainage04.brainagelib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.gametest.CapturedCommandOutput;
import io.github.brainage04.brainagelib.platform.ServerPlatform;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Listeners a mod registers through {@link ServerPlatform} while it initializes run on the GameTest
 * server at the moments the platform documents, on both loaders.
 */
public final class ServerPlatformGameTests {
    private static final String PROBE_COMMAND = "brainagelib_platform_probe";
    private static final int PROBE_RESULT = 7;
    private static final AtomicInteger END_TICKS = new AtomicInteger();
    private static volatile MinecraftServer startingServer;
    private static volatile boolean levelsLoadedWhenStarting;
    private static volatile MinecraftServer startedServer;
    private static volatile boolean levelsLoadedWhenStarted;
    private static volatile Commands.CommandSelection commandSelection;

    private ServerPlatformGameTests() {}

    /** Called by each loader's GameTest mod while it initializes, as a mod would. */
    public static void registerListeners() {
        ServerPlatform platform = ServerPlatform.get();
        platform.onServerStarting(
                server -> {
                    startingServer = server;
                    levelsLoadedWhenStarting = server.getAllLevels().iterator().hasNext();
                });
        platform.onServerStarted(
                server -> {
                    startedServer = server;
                    levelsLoadedWhenStarted = server.getAllLevels().iterator().hasNext();
                });
        platform.onEndServerTick(server -> END_TICKS.incrementAndGet());
        platform.onRegisterCommands(
                (dispatcher, context, selection) -> {
                    commandSelection = selection;
                    dispatcher.register(
                            Commands.literal(PROBE_COMMAND).executes(command -> PROBE_RESULT));
                });
    }

    public static void listenersRunOnTheRunningServer(GameTestHelper context, String loaderName) {
        MinecraftServer server = context.getLevel().getServer();
        ServerPlatform platform = ServerPlatform.get();
        context.assertValueEqual(
                platform.loaderName(), loaderName, "The loader's own platform is used");
        context.assertTrue(
                Files.isDirectory(platform.configDirectory()), "The config directory exists");

        context.assertTrue(startingServer == server, "Starting listeners get this server");
        context.assertFalse(
                levelsLoadedWhenStarting,
                "Starting listeners run before the server loads its levels");
        context.assertTrue(startedServer == server, "Started listeners get this server");
        context.assertTrue(
                levelsLoadedWhenStarted,
                "Started listeners run once the server has loaded its levels");

        // A GameTest server loads its world with the dedicated selection (dec/server-26.3
        // GameTestServer.java:110), as a dedicated server does (Main.java:301).
        context.assertTrue(
                commandSelection == Commands.CommandSelection.DEDICATED,
                "Command registrars get the server's command selection, got " + commandSelection);

        var output = new CapturedCommandOutput();
        try {
            context.assertValueEqual(
                    server.getCommands()
                            .getDispatcher()
                            .execute(PROBE_COMMAND, output.source(server)),
                    PROBE_RESULT,
                    "The registered command runs");
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("The registered command must exist", exception);
        }

        int endTicksAtStart = END_TICKS.get();
        int serverTickAtStart = server.getTickCount();
        context.runAfterDelay(
                3,
                () -> {
                    context.assertValueEqual(
                            END_TICKS.get() - endTicksAtStart,
                            server.getTickCount() - serverTickAtStart,
                            "End-of-tick listeners run once per server tick");
                    context.succeed();
                });
    }
}
