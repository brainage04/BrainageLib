package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.BrainageLibGameTests;
import io.github.brainage04.brainagelib.DropCaptureGameTests;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = BrainageLib.MOD_ID)
public final class BrainageLibNeoForgeGameTests {
    private static final List<TestFunction> TEST_FUNCTIONS =
            List.of(
                    new TestFunction(
                            "help_commands_execute_and_restrict_configuration",
                            BrainageLibGameTests::helpCommandsExecuteAndRestrictConfiguration),
                    new TestFunction(
                            "feedback_delivers_formatted_messages_and_results",
                            BrainageLibGameTests::feedbackDeliversFormattedMessagesAndResults),
                    new TestFunction(
                            "first_join_acknowledgement_survives_reload",
                            BrainageLibGameTests::firstJoinAcknowledgementSurvivesReload),
                    new TestFunction(
                            "unreadable_first_join_state_is_not_overwritten",
                            BrainageLibGameTests::unreadableFirstJoinStateIsNotOverwritten),
                    new TestFunction(
                            "first_join_state_from_previous_version_loads",
                            BrainageLibGameTests::firstJoinStateFromPreviousVersionLoads),
                    new TestFunction(
                            "game_test_player_joins_runs_commands_and_cleans_up",
                            BrainageLibGameTests::gameTestPlayerJoinsRunsCommandsAndCleansUp),
                    new TestFunction(
                            "highest_governing_drop_policy_decides",
                            DropCaptureGameTests::highestGoverningPolicyDecides),
                    new TestFunction(
                            "ungoverned_player_keeps_drops_in_world",
                            DropCaptureGameTests::ungovernedPlayerKeepsDropsInWorld),
                    new TestFunction(
                            "refused_drop_stays_where_it_spawned",
                            DropCaptureGameTests::refusedStackStaysWhereItSpawned),
                    new TestFunction(
                            "block_broken_for_a_player_elsewhere_is_captured",
                            DropCaptureGameTests::blockBrokenForAPlayerElsewhereIsCaptured),
                    new TestFunction(
                            "felled_leaf_decay_pays_the_feller",
                            DropCaptureGameTests::felledLeafDecayPaysTheFeller),
                    new TestFunction(
                            "player_log_break_records_its_leaves",
                            DropCaptureGameTests::playerLogBreakRecordsItsLeaves),
                    new TestFunction(
                            "respawn_anchor_click_credits_the_clicker",
                            DropCaptureGameTests::respawnAnchorClickCreditsTheClicker),
                    new TestFunction(
                            "bed_click_credits_the_clicker",
                            DropCaptureGameTests::bedClickCreditsTheClicker),
                    new TestFunction(
                            "armor_stand_broken_by_player_is_captured",
                            DropCaptureGameTests::armorStandBrokenByPlayerIsCaptured),
                    new TestFunction(
                            "armor_stand_broken_by_a_blast_stays_in_the_world",
                            DropCaptureGameTests::armorStandBrokenByABlastStaysInTheWorld),
                    new TestFunction(
                            "armor_stand_broken_by_a_players_blast_is_captured",
                            DropCaptureGameTests::armorStandBrokenByAPlayersBlastIsCaptured),
                    new TestFunction(
                            "item_frames_broken_by_player_are_captured",
                            DropCaptureGameTests::itemFramesBrokenByPlayerAreCaptured),
                    new TestFunction(
                            "cushion_broken_by_player_is_captured",
                            DropCaptureGameTests::cushionBrokenByPlayerIsCaptured),
                    new TestFunction(
                            "painting_broken_by_player_is_captured",
                            DropCaptureGameTests::paintingBrokenByPlayerIsCaptured),
                    new TestFunction(
                            "decoration_broken_without_a_policy_stays_in_the_world",
                            DropCaptureGameTests::decorationBrokenWithoutAPolicyStaysInTheWorld),
                    new TestFunction(
                            "match_policy_governs_decoration_breaks",
                            DropCaptureGameTests::matchPolicyGovernsDecorationBreaks),
                    new TestFunction(
                            "kill_credited_without_a_player_hit_goes_to_the_killer",
                            DropCaptureGameTests::killCreditedWithoutAPlayerHitGoesToTheKiller),
                    new TestFunction(
                            "kill_no_player_is_credited_with_stays_in_the_world",
                            DropCaptureGameTests::killNoPlayerIsCreditedWithStaysInTheWorld),
                    new TestFunction(
                            "vehicles_broken_by_player_are_captured",
                            DropCaptureGameTests::vehiclesBrokenByPlayerAreCaptured),
                    new TestFunction(
                            "minecarts_broken_by_player_are_captured",
                            DropCaptureGameTests::minecartsBrokenByPlayerAreCaptured),
                    new TestFunction(
                            "vehicle_broken_without_a_player_stays_in_the_world",
                            DropCaptureGameTests::vehicleBrokenWithoutAPlayerStaysInTheWorld),
                    new TestFunction(
                            "creative_break_of_vehicle_stays_in_the_world",
                            DropCaptureGameTests::creativeBreakOfVehicleStaysInTheWorld),
                    new TestFunction(
                            "vehicle_broken_by_a_player_who_is_not_delivered_stays_in_the_world",
                            DropCaptureGameTests::vehicleBrokenByAPlayerWhoIsNotDeliveredStaysInTheWorld));

    private BrainageLibNeoForgeGameTests() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        for (TestFunction testFunction : TEST_FUNCTIONS) {
            event.register(
                    BuiltInRegistries.TEST_FUNCTION.key(),
                    Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, testFunction.id()),
                    testFunction::function);
        }
    }

    private record TestFunction(String id, Consumer<GameTestHelper> function) {}
}
