package io.github.brainage04.brainagelib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Listens to Fabric's server-starting event for {@link CurrentServerGameTests}. */
public final class BrainageLibGameTestServerStarting implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(CurrentServerGameTests::serverStarting);
    }
}
