package io.github.brainage04.brainagelib.fabric;

import io.github.brainage04.brainagelib.platform.ServerPlatform;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/** {@link ServerPlatform} on Fabric, registered as a service in {@code META-INF/services}. */
public final class FabricServerPlatform implements ServerPlatform {
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void onRegisterCommands(CommandRegistrar registrar) {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, context, selection) -> registrar.register(dispatcher, context));
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STARTING.register(listener::accept);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STARTED.register(listener::accept);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STOPPING.register(listener::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STOPPED.register(listener::accept);
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> listener) {
        ServerTickEvents.END_SERVER_TICK.register(listener::accept);
    }
}
