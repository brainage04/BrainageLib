package io.github.brainage04.brainagelib.neoforge;

import io.github.brainage04.brainagelib.BrainageLib;
import io.github.brainage04.brainagelib.command.ServerModsCommand;
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
        Identifier id = Identifier.fromNamespaceAndPath(BrainageLib.MOD_ID, "servermods_command_is_registered");
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), id, () -> context -> {
            if (context.getLevel().getServer().getCommands().getDispatcher().getRoot()
                    .getChild(ServerModsCommand.COMMAND_NAME) == null) {
                throw new AssertionError("Expected the servermods command to be registered on the NeoForge dedicated server.");
            }
            context.succeed();
        });
    }
}
