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

    @GameTest
    public void unreadableFirstJoinStateIsNotOverwritten(GameTestHelper context) {
        BrainageLibGameTests.unreadableFirstJoinStateIsNotOverwritten(context);
    }

    @GameTest
    public void firstJoinStateFromPreviousVersionLoads(GameTestHelper context) {
        BrainageLibGameTests.firstJoinStateFromPreviousVersionLoads(context);
    }

    @GameTest
    public void gameTestPlayerJoinsRunsCommandsAndCleansUp(GameTestHelper context) {
        BrainageLibGameTests.gameTestPlayerJoinsRunsCommandsAndCleansUp(context);
    }

    @GameTest
    public void highestGoverningPolicyDecides(GameTestHelper context) {
        DropCaptureGameTests.highestGoverningPolicyDecides(context);
    }

    @GameTest
    public void ungovernedPlayerKeepsDropsInWorld(GameTestHelper context) {
        DropCaptureGameTests.ungovernedPlayerKeepsDropsInWorld(context);
    }

    @GameTest
    public void refusedStackStaysWhereItSpawned(GameTestHelper context) {
        DropCaptureGameTests.refusedStackStaysWhereItSpawned(context);
    }

    @GameTest
    public void blockBrokenForAPlayerElsewhereIsCaptured(GameTestHelper context) {
        DropCaptureGameTests.blockBrokenForAPlayerElsewhereIsCaptured(context);
    }

    @GameTest
    public void felledLeafDecayPaysTheFeller(GameTestHelper context) {
        DropCaptureGameTests.felledLeafDecayPaysTheFeller(context);
    }

    @GameTest
    public void playerLogBreakRecordsItsLeaves(GameTestHelper context) {
        DropCaptureGameTests.playerLogBreakRecordsItsLeaves(context);
    }

    @GameTest(maxTicks = 100)
    public void respawnAnchorClickCreditsTheClicker(GameTestHelper context) {
        DropCaptureGameTests.respawnAnchorClickCreditsTheClicker(context);
    }

    @GameTest(maxTicks = 100)
    public void bedClickCreditsTheClicker(GameTestHelper context) {
        DropCaptureGameTests.bedClickCreditsTheClicker(context);
    }
}
