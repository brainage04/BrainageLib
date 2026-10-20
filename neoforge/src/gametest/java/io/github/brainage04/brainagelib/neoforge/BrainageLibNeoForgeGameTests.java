package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.BrainageLibGameTests;
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
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "help_commands_execute_and_restrict_configuration"),
                () -> BrainageLibGameTests::helpCommandsExecuteAndRestrictConfiguration);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "feedback_delivers_formatted_messages_and_results"),
                () -> BrainageLibGameTests::feedbackDeliversFormattedMessagesAndResults);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "first_join_acknowledgement_survives_reload"),
                () -> BrainageLibGameTests::firstJoinAcknowledgementSurvivesReload);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "unreadable_first_join_state_is_not_overwritten"),
                () -> BrainageLibGameTests::unreadableFirstJoinStateIsNotOverwritten);
    }
}
