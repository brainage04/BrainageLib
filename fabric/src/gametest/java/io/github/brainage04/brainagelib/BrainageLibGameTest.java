package io.github.brainage04.brainagelib;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class BrainageLibGameTest {
    @GameTest
    public void helpCommandsExecuteAndRestrictConfiguration(GameTestHelper context) {
        BrainageLibGameTests.helpCommandsExecuteAndRestrictConfiguration(context);
    }

    @GameTest
    public void feedbackDeliversFormattedMessagesAndResults(GameTestHelper context) {
        BrainageLibGameTests.feedbackDeliversFormattedMessagesAndResults(context);
    }

    @GameTest
    public void firstJoinAcknowledgementSurvivesReload(GameTestHelper context) {
        BrainageLibGameTests.firstJoinAcknowledgementSurvivesReload(context);
    }
}
