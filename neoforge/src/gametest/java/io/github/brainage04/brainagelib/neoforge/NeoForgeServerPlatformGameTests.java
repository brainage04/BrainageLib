package io.github.brainage04.brainagelib.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Each listener goes to the NeoForge event with the meaning Fabric's event has. The platform is
 * built on a bus of its own so that posting the events here reaches no other listener; a server's
 * stop cannot be observed from a GameTest otherwise.
 */
public final class NeoForgeServerPlatformGameTests {
    private NeoForgeServerPlatformGameTests() {}

    public static void listensToTheMatchingEvents(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        IEventBus bus = BusBuilder.builder().build();
        NeoForgeServerPlatform platform = new NeoForgeServerPlatform(bus);
        List<String> calls = new ArrayList<>();
        platform.onServerStarting(listened -> calls.add(listened == server ? "starting" : "?"));
        platform.onServerStarted(listened -> calls.add(listened == server ? "started" : "?"));
        platform.onServerStopping(listened -> calls.add(listened == server ? "stopping" : "?"));
        platform.onServerStopped(listened -> calls.add(listened == server ? "stopped" : "?"));
        platform.onEndServerTick(listened -> calls.add(listened == server ? "end tick" : "?"));
        platform.onRegisterCommands(
                (dispatcher, buildContext, selection) -> calls.add("commands " + selection));

        bus.post(new ServerStartingEvent(server));
        bus.post(new ServerTickEvent.Pre(() -> true, server));
        context.assertValueEqual(
                calls,
                List.of(),
                "Calls for the events posted after the levels load and at the start of a tick");

        bus.post(new ServerAboutToStartEvent(server));
        bus.post(new ServerStartedEvent(server));
        bus.post(new ServerTickEvent.Post(() -> true, server));
        bus.post(new ServerStoppingEvent(server));
        bus.post(new ServerStoppedEvent(server));
        context.assertValueEqual(
                calls,
                List.of("starting", "started", "end tick", "stopping", "stopped"),
                "Calls in the order of the events");

        // After a normal stop NeoForge reaches stopServer after ServerStoppingEvent; after a crash
        // or a failed start it reaches stopServer without that event.
        calls.clear();
        bus.post(new ServerStoppingEvent(server));
        platform.beforeStopServer(server);
        bus.post(new ServerStoppedEvent(server));
        context.assertValueEqual(
                calls, List.of("stopping", "stopped"), "Calls for a normal stop: stopping once");

        calls.clear();
        platform.beforeStopServer(server);
        bus.post(new ServerStoppedEvent(server));
        context.assertValueEqual(
                calls,
                List.of("stopping", "stopped"),
                "Calls for a stop without ServerStoppingEvent (a crash or a failed start)");

        calls.clear();
        bus.post(
                new RegisterCommandsEvent(
                        new CommandDispatcher<>(),
                        Commands.CommandSelection.INTEGRATED,
                        Commands.createValidationContext(server.registryAccess())));
        context.assertValueEqual(
                calls,
                List.of("commands INTEGRATED"),
                "Command registrars get the event's command selection");

        IEventBus failingBus = BusBuilder.builder().build();
        NeoForgeServerPlatform failing = new NeoForgeServerPlatform(failingBus);
        failing.onServerStopping(
                listened -> {
                    throw new IllegalStateException("A stopping listener failed on purpose");
                });
        // A Kotlin listener or @SneakyThrows can throw a checked exception through a Consumer.
        failing.onServerStopping(
                listened -> sneakyThrow(new IOException("A stopping listener failed on purpose")));
        failing.onServerStopping(listened -> calls.add("stopping"));
        // The bus rethrows a listener's exception, which would end a normal stop as a crash and
        // skip the listeners after it.
        calls.clear();
        failingBus.post(new ServerStoppingEvent(server));
        failing.beforeStopServer(server);
        context.assertValueEqual(
                calls,
                List.of("stopping"),
                "A stopping listener that throws in ServerStoppingEvent leaves the others to run");

        calls.clear();
        failing.beforeStopServer(server);
        context.assertValueEqual(
                calls,
                List.of("stopping"),
                "A stopping listener that throws leaves the others to run");
        context.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
