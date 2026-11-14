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

    @GameTest
    public void armorStandBrokenByPlayerIsCaptured(GameTestHelper context) {
        DropCaptureGameTests.armorStandBrokenByPlayerIsCaptured(context);
    }

    @GameTest
    public void armorStandBrokenByABlastStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.armorStandBrokenByABlastStaysInTheWorld(context);
    }

    @GameTest
    public void armorStandBrokenByAPlayersBlastIsCaptured(GameTestHelper context) {
        DropCaptureGameTests.armorStandBrokenByAPlayersBlastIsCaptured(context);
    }

    @GameTest
    public void itemFramesBrokenByPlayerAreCaptured(GameTestHelper context) {
        DropCaptureGameTests.itemFramesBrokenByPlayerAreCaptured(context);
    }

    @GameTest
    public void cushionBrokenByPlayerIsCaptured(GameTestHelper context) {
        DropCaptureGameTests.cushionBrokenByPlayerIsCaptured(context);
    }

    @GameTest
    public void paintingBrokenByPlayerIsCaptured(GameTestHelper context) {
        DropCaptureGameTests.paintingBrokenByPlayerIsCaptured(context);
    }

    @GameTest
    public void decorationBrokenWithoutAPolicyStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.decorationBrokenWithoutAPolicyStaysInTheWorld(context);
    }

    @GameTest
    public void matchPolicyGovernsDecorationBreaks(GameTestHelper context) {
        DropCaptureGameTests.matchPolicyGovernsDecorationBreaks(context);
    }

    @GameTest
    public void killCreditedWithoutAPlayerHitGoesToTheKiller(GameTestHelper context) {
        DropCaptureGameTests.killCreditedWithoutAPlayerHitGoesToTheKiller(context);
    }

    @GameTest
    public void killNoPlayerIsCreditedWithStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.killNoPlayerIsCreditedWithStaysInTheWorld(context);
    }

    @GameTest
    public void killCreditedToAMobStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.killCreditedToAMobStaysInTheWorld(context);
    }

    @GameTest
    public void vehiclesBrokenByPlayerAreCaptured(GameTestHelper context) {
        DropCaptureGameTests.vehiclesBrokenByPlayerAreCaptured(context);
    }

    @GameTest
    public void minecartsBrokenByPlayerAreCaptured(GameTestHelper context) {
        DropCaptureGameTests.minecartsBrokenByPlayerAreCaptured(context);
    }

    @GameTest
    public void vehicleBrokenWithoutAPlayerStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.vehicleBrokenWithoutAPlayerStaysInTheWorld(context);
    }

    @GameTest
    public void creativeBreakOfVehicleStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.creativeBreakOfVehicleStaysInTheWorld(context);
    }

    @GameTest
    public void vehicleBrokenByAPlayerWhoIsNotDeliveredStaysInTheWorld(GameTestHelper context) {
        DropCaptureGameTests.vehicleBrokenByAPlayerWhoIsNotDeliveredStaysInTheWorld(context);
    }

    @GameTest
    public void playerIdCodecsDamagedPlayerKeyLosesOnlyThatPlayer(GameTestHelper context) {
        PlayerIdCodecsGameTests.damagedPlayerKeyLosesOnlyThatPlayer(context);
    }
}
