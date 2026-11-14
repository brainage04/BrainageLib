package io.github.brainage04.brainagelib.server;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * The server running in this game instance, for code the game gives no server or level to (a static
 * item method, worldgen settings read on worldgen threads). Code that has a level or a player
 * should use {@code level.getServer()} instead.
 *
 * <p>A server is current from the moment its thread starts, before it loads its levels and before
 * any starting listener runs, until its thread ends, after every stopped listener. In a client it
 * is the singleplayer (integrated) server while one runs, and {@code null} on a remote server.
 */
public final class CurrentServer {
    private static final ServerSlot<MinecraftServer> SLOT = new ServerSlot<>();

    private CurrentServer() {}

    /** The running server, or {@code null} when none runs in this game instance. */
    public static @Nullable MinecraftServer get() {
        return SLOT.get();
    }

    /** Called by BrainageLib's own mixin when a server thread starts; mods must not call it. */
    public static void serverThreadStarted(MinecraftServer server) {
        SLOT.start(server);
    }

    /**
     * Called by BrainageLib's own mixin when a server thread ends; mods must not call it. A server
     * that is no longer current (a new one has started) leaves the new one in place.
     */
    public static void serverThreadEnded(MinecraftServer server) {
        SLOT.stop(server);
    }
}
