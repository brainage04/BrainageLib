package io.github.brainage04.brainagelib.help;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerModHelpRegistryTest {
    @Test
    void aggregatesAndSortsInstalledModHelp() {
        ServerModHelpEntry zeta = new ServerModHelpEntry(
                "registry_test_zeta",
                "Zeta Mod",
                "Zeta description.",
                "/zeta help",
                "/zeta config"
        );
        ServerModHelpEntry alpha = ServerModHelpEntry.playerOnly(
                "registry_test_alpha",
                "Alpha Mod",
                "Alpha description.",
                "/alpha help"
        );

        ServerModHelpRegistry.register(zeta);
        ServerModHelpRegistry.register(alpha);

        List<ServerModHelpEntry> testEntries = ServerModHelpRegistry.entries().stream()
                .filter(entry -> entry.modId().startsWith("registry_test_"))
                .toList();
        assertEquals(List.of(alpha, zeta), testEntries);

        String playerMessage = ServerModHelpRegistry.firstJoinMessage(false).getString();
        assertTrue(playerMessage.contains("Alpha Mod and Zeta Mod"));
        assertTrue(playerMessage.contains("/servermods help"));

        String operatorMessage = ServerModHelpRegistry.firstJoinMessage(true).getString();
        assertTrue(operatorMessage.contains("/servermods config"));
    }
}
