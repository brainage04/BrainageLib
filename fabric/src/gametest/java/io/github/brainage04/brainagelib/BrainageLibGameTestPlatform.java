package io.github.brainage04.brainagelib;

import net.fabricmc.api.ModInitializer;

/** Registers the GameTest platform listeners while the mod initializes, as a Fabric mod would. */
public final class BrainageLibGameTestPlatform implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlatformGameTests.registerListeners();
    }
}
