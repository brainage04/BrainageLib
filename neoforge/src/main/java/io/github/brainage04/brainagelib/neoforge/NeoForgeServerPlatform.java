package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.platform.ServerPlatform;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/** {@link ServerPlatform} on NeoForge, registered as a service in {@code META-INF/services}. */
public final class NeoForgeServerPlatform implements ServerPlatform {
    private final IEventBus bus;
    private final List<Consumer<MinecraftServer>> stoppingListeners = new CopyOnWriteArrayList<>();
    private volatile @Nullable MinecraftServer stoppingPostedFor;

    public NeoForgeServerPlatform() {
        this(NeoForge.EVENT_BUS);
    }

    NeoForgeServerPlatform(IEventBus bus) {
        this.bus = bus;
    }

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void onRegisterCommands(CommandRegistrar registrar) {
        bus.addListener(
                (RegisterCommandsEvent event) ->
                        registrar.register(
                                event.getDispatcher(),
                                event.getBuildContext(),
                                event.getCommandSelection()));
    }

    // Not ServerStartingEvent: NeoForge posts it after initServer has loaded the levels
    // (ServerLifecycleHooks.handleServerStarting, called after loadLevel, which is at
    // dec/server-26.3 DedicatedServer.java:245 and GameTestServer.java:179, dec/client-26.3
    // IntegratedServer.java:103). Fabric's SERVER_STARTING runs before initServer
    // (fabric-lifecycle-events-v1 4.1.9 MinecraftServerMixin.beforeSetupServer); the about-to-start
    // event is the one that also comes before loadLevel.
    @Override
    public void onServerStarting(Consumer<MinecraftServer> listener) {
        bus.addListener((ServerAboutToStartEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        bus.addListener((ServerStartedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> listener) {
        stoppingListeners.add(listener);
        bus.addListener(
                (ServerStoppingEvent event) -> {
                    stoppingPostedFor = event.getServer();
                    listener.accept(event.getServer());
                });
    }

    /**
     * Called by BrainageLib's NeoForge mixin at the head of {@code MinecraftServer.stopServer};
     * mods must not call it. Runs the stopping listeners unless {@code ServerStoppingEvent} already
     * ran them for this server.
     */
    public void beforeStopServer(MinecraftServer server) {
        // NeoForge posts ServerStoppingEvent only when the tick loop ends normally (userdev
        // 26.3.0.99 MinecraftServer.java.patch:78, inside runServer's try), not after a crash or a
        // failed start. Fabric's SERVER_STOPPING runs at the head of stopServer
        // (fabric-lifecycle-events-v1 4.1.9 MinecraftServerMixin.beforeShutdownServer), which
        // runServer's finally reaches on every path.
        if (stoppingPostedFor != server) {
            for (Consumer<MinecraftServer> listener : stoppingListeners) {
                listener.accept(server);
            }
        }
        stoppingPostedFor = null;
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        bus.addListener((ServerStoppedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> listener) {
        bus.addListener((ServerTickEvent.Post event) -> listener.accept(event.getServer()));
    }
}
