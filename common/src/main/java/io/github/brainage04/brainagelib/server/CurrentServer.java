package io.github.brainage04.brainagelib.server;

import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * The server running in this game instance, for server code the game gives no server or level to (a
 * static item method, worldgen settings read on worldgen threads). Code that has a level or a
 * player should use {@code level.getServer()} instead.
 *
 * <p>A server is current from before it loads its levels and before any starting listener runs
 * until after every stopped listener has run.
 *
 * <p>On NeoForge the value is the loader's own record, which NeoForge sets for the dedicated,
 * integrated and GameTest servers only: a mod that starts a {@code MinecraftServer} subclass of its
 * own gets {@code null} there. On Fabric a mixin covers every server.
 *
 * <p>Call it only from code that runs for the server: its own thread or the background executor it
 * runs world generation on. Client code must not branch on it: in a client it is the singleplayer
 * (integrated) server while one runs and {@code null} on a remote server, so the same client code
 * would behave differently in singleplayer and on a server.
 */
public final class CurrentServer {
    private static final ServerSlot<MinecraftServer> SLOT = new ServerSlot<>();
    private static volatile Supplier<@Nullable MinecraftServer> lookup = SLOT::get;

    private CurrentServer() {}

    /** The running server, or {@code null} when none runs in this game instance. */
    public static @Nullable MinecraftServer get() {
        return lookup.get();
    }

    /**
     * Called by BrainageLib's NeoForge module to answer {@link #get()} with the loader's own record
     * of the running server. BrainageLib's own wiring: a mod that calls it changes the answer for
     * every mod. On NeoForge that record is a plain field, so a read from another thread sees it no
     * sooner than NeoForge publishes it.
     */
    @ApiStatus.Internal
    public static void readFrom(Supplier<@Nullable MinecraftServer> loaderLookup) {
        lookup = loaderLookup;
    }

    /** Called by BrainageLib's Fabric mixin when a server thread starts. */
    @ApiStatus.Internal
    public static void serverThreadStarted(MinecraftServer server) {
        SLOT.start(server);
    }

    /**
     * Called by BrainageLib's Fabric mixin when a server thread ends. A server that is no longer
     * current (a new one has started) leaves the new one in place.
     */
    @ApiStatus.Internal
    public static void serverThreadEnded(MinecraftServer server) {
        SLOT.stop(server);
    }
}
