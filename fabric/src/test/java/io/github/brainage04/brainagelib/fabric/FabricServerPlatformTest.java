package io.github.brainage04.brainagelib.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
        platform.onServerStopping(server -> CALLS.add("stopping"));
        platform.onServerStopped(server -> CALLS.add("stopped"));
        platform.onEndServerTick(server -> CALLS.add("end tick"));
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
    void stoppingListensToServerStopping() {
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(null);
        assertEquals(List.of("stopping"), CALLS);
    }

    @Test
    void stoppedListensToServerStopped() {
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(null);
        assertEquals(List.of("stopped"), CALLS);
    }

    @Test
    void endTickListensToEndServerTick() {
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(null);
        assertEquals(List.of("end tick"), CALLS);
    }
}
