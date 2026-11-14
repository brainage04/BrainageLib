package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.server.CurrentServer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/** {@link CurrentServer} is the GameTest server, already while the server is starting. */
public final class CurrentServerGameTests {
    private static volatile MinecraftServer currentWhenStarting;
    private static volatile MinecraftServer starting;

    private CurrentServerGameTests() {}

    /** Called by each loader's GameTest mod from the loader's first server-starting event. */
    public static void serverStarting(MinecraftServer server) {
        starting = server;
        currentWhenStarting = CurrentServer.get();
    }

    public static void currentServerIsTheRunningServer(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        context.assertTrue(CurrentServer.get() == server, "The running server is current");
        context.assertTrue(starting == server, "The starting event reached the GameTest mod");
        context.assertTrue(
                currentWhenStarting == server,
                "The server is current before the loader's starting listeners run");
        context.succeed();
    }
}
