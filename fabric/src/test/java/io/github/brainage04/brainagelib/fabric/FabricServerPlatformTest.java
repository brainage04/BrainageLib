package io.github.brainage04.brainagelib.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Each listener goes to the Fabric event with the same meaning. The in-game half (starting,
 * started, commands and ticks fired by a real server) is {@code ServerPlatformGameTests}; a
 * server's stop cannot be observed from a GameTest, so the stop events are fired here.
 */
class FabricServerPlatformTest {
    private static final List<String> CALLS = new ArrayList<>();

    @BeforeAll
    static void registerOneListenerPerEvent() {
        FabricServerPlatform platform = new FabricServerPlatform();
        platform.onServerStarting(server -> CALLS.add("starting"));
        platform.onServerStarted(server -> CALLS.add("started"));
        // Fabric's invoker runs the listeners in a plain loop at the head of stopServer, so a
        // throw that got out would also skip the listeners after it and the save.
        platform.onServerStopping(
                server -> {
                    throw new IllegalStateException("A stopping listener failed on purpose");
                });
        // A Kotlin listener or @SneakyThrows can throw a checked exception through a Consumer.
        platform.onServerStopping(
                server -> sneakyThrow(new IOException("A stopping listener failed on purpose")));
        platform.onServerStopping(server -> CALLS.add("stopping"));
        platform.onServerStopped(server -> CALLS.add("stopped"));
        platform.onEndServerTick(server -> CALLS.add("end tick"));
        platform.onRegisterCommands(
                (dispatcher, context, selection) -> CALLS.add("commands " + selection));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    @BeforeEach
    void forgetEarlierCalls() {
        CALLS.clear();
    }

    @Test
    void startingListensToServerStarting() {
        ServerLifecycleEvents.SERVER_STARTING.invoker().onServerStarting(null);
        assertEquals(List.of("starting"), CALLS);
    }

    @Test
    void startedListensToServerStarted() {
        ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(null);
        assertEquals(List.of("started"), CALLS);
    }

    @Test
    void stoppingListensToServerStoppingPastAListenerThatThrows() {
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(null);
        assertEquals(List.of("stopping"), CALLS);
    }

    @Test
    void stoppedListensToServerStopped() {
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(null);
        assertEquals(List.of("stopped"), CALLS);
    }

    @Test
    void commandRegistrarsGetTheCommandSelection() {
        CommandRegistrationCallback.EVENT
                .invoker()
                .register(null, null, Commands.CommandSelection.INTEGRATED);
        assertEquals(List.of("commands INTEGRATED"), CALLS);
    }

    @Test
    void endTickListensToEndServerTick() {
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(null);
        assertEquals(List.of("end tick"), CALLS);
    }
}
