package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.BrainageLibGameTests;
import io.github.brainage04.brainagelib.DropCaptureGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = BrainageLib.MOD_ID)
public final class BrainageLibNeoForgeGameTests {
    private BrainageLibNeoForgeGameTests() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "help_commands_execute_and_restrict_configuration"),
                () -> BrainageLibGameTests::helpCommandsExecuteAndRestrictConfiguration);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "feedback_delivers_formatted_messages_and_results"),
                () -> BrainageLibGameTests::feedbackDeliversFormattedMessagesAndResults);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "first_join_acknowledgement_survives_reload"),
                () -> BrainageLibGameTests::firstJoinAcknowledgementSurvivesReload);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "unreadable_first_join_state_is_not_overwritten"),
                () -> BrainageLibGameTests::unreadableFirstJoinStateIsNotOverwritten);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "first_join_state_from_previous_version_loads"),
                () -> BrainageLibGameTests::firstJoinStateFromPreviousVersionLoads);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "game_test_player_joins_runs_commands_and_cleans_up"),
                () -> BrainageLibGameTests::gameTestPlayerJoinsRunsCommandsAndCleansUp);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "highest_governing_drop_policy_decides"),
                () -> DropCaptureGameTests::highestGoverningPolicyDecides);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "ungoverned_player_keeps_drops_in_world"),
                () -> DropCaptureGameTests::ungovernedPlayerKeepsDropsInWorld);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "refused_drop_stays_where_it_spawned"),
                () -> DropCaptureGameTests::refusedStackStaysWhereItSpawned);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "block_broken_for_a_player_elsewhere_is_captured"),
                () -> DropCaptureGameTests::blockBrokenForAPlayerElsewhereIsCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "felled_leaf_decay_pays_the_feller"),
                () -> DropCaptureGameTests::felledLeafDecayPaysTheFeller);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "player_log_break_records_its_leaves"),
                () -> DropCaptureGameTests::playerLogBreakRecordsItsLeaves);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "respawn_anchor_click_credits_the_clicker"),
                () -> DropCaptureGameTests::respawnAnchorClickCreditsTheClicker);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "bed_click_credits_the_clicker"),
                () -> DropCaptureGameTests::bedClickCreditsTheClicker);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "armor_stand_broken_by_player_is_captured"),
                () -> DropCaptureGameTests::armorStandBrokenByPlayerIsCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "armor_stand_broken_by_a_blast_stays_in_the_world"),
                () -> DropCaptureGameTests::armorStandBrokenByABlastStaysInTheWorld);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "armor_stand_broken_by_a_players_blast_is_captured"),
                () -> DropCaptureGameTests::armorStandBrokenByAPlayersBlastIsCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "item_frames_broken_by_player_are_captured"),
                () -> DropCaptureGameTests::itemFramesBrokenByPlayerAreCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "cushion_broken_by_player_is_captured"),
                () -> DropCaptureGameTests::cushionBrokenByPlayerIsCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "painting_broken_by_player_is_captured"),
                () -> DropCaptureGameTests::paintingBrokenByPlayerIsCaptured);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID,
                        "decoration_broken_without_a_policy_stays_in_the_world"),
                () -> DropCaptureGameTests::decorationBrokenWithoutAPolicyStaysInTheWorld);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "match_policy_governs_decoration_breaks"),
                () -> DropCaptureGameTests::matchPolicyGovernsDecorationBreaks);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID,
                        "kill_credited_without_a_player_hit_goes_to_the_killer"),
                () -> DropCaptureGameTests::killCreditedWithoutAPlayerHitGoesToTheKiller);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BrainageLib.MOD_ID, "kill_no_player_is_credited_with_stays_in_the_world"),
                () -> DropCaptureGameTests::killNoPlayerIsCreditedWithStaysInTheWorld);
    }
}
