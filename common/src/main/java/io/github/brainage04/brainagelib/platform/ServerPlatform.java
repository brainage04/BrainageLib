package io.github.brainage04.brainagelib.platform;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * The loader services a server-side mod needs from common code: the config directory, command
 * registration and the server's lifecycle and tick, with the same meaning on Fabric and NeoForge.
 *
 * <p>Call {@link #get()} from common code; BrainageLib's Fabric and NeoForge modules provide the
 * instance. Register listeners while the mod initializes. Each listener is added to the loader's
 * own event when it is registered, so listeners run in the order the loader runs its events.
 */
public interface ServerPlatform {
    /** The platform of the loader that is running, found once and then reused. */
    static ServerPlatform get() {
        return ServerPlatforms.INSTANCE;
    }

    /** {@code "Fabric"} or {@code "NeoForge"}. */
    String loaderName();

    /** The directory the loader keeps mods' config files in. */
    Path configDirectory();

    /** Runs {@code registrar} every time the server builds its command tree. */
    void onRegisterCommands(CommandRegistrar registrar);

    /**
     * Runs {@code listener} when a server starts, before it loads its levels: Fabric's {@code
     * SERVER_STARTING} and NeoForge's {@code ServerAboutToStartEvent}.
     */
    void onServerStarting(Consumer<MinecraftServer> listener);

    /** Runs {@code listener} once the server has loaded its levels, before its first tick. */
    void onServerStarted(Consumer<MinecraftServer> listener);

    /** Runs {@code listener} after the server's last tick, before it saves and shuts down. */
    void onServerStopping(Consumer<MinecraftServer> listener);

    /** Runs {@code listener} after the server has saved and shut down. */
    void onServerStopped(Consumer<MinecraftServer> listener);

    /** Runs {@code listener} at the end of every server tick. */
    void onEndServerTick(Consumer<MinecraftServer> listener);

    /** Adds a mod's commands to the server's command tree. */
    @FunctionalInterface
    interface CommandRegistrar {
        void register(
                CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context);
    }
}
