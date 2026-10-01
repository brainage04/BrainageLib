package io.github.brainage04.brainagelib;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class BrainageLibGameTest {
    @GameTest
    public void serverModsCommandIsRegistered(GameTestHelper context) {
        BrainageLibGameTests.serverModsCommandIsRegistered(context);
    }
}
