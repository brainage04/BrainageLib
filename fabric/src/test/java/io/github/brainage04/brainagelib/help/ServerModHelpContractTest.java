package io.github.brainage04.brainagelib.help;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerModHelpContractTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void duplicateRegistrationIsIdempotentButConflictsDoNotReplaceHelp() {
        ServerModHelpEntry entry = ServerModHelpEntry.playerOnly("contract_duplicate", "Original", "Description", "/original help");
        ServerModHelpEntry conflict = ServerModHelpEntry.playerOnly("contract_duplicate", "Replacement", "Description", "/replacement help");
        ServerModHelpRegistry.register(entry);
        try {
            ServerModHelpRegistry.register(entry);
            assertThrows(IllegalStateException.class, () -> ServerModHelpRegistry.register(conflict));
            assertFalse(ServerModHelpRegistry.unregister(conflict));
            assertEquals(1, ServerModHelpRegistry.entries().stream().filter(entry::equals).count());
            assertFalse(ServerModHelpRegistry.entries().contains(conflict));
            assertThrows(UnsupportedOperationException.class, () -> ServerModHelpRegistry.entries().clear());
        } finally {
            ServerModHelpRegistry.unregister(entry);
        }
    }

    @Test
    void firstJoinNoticeShowsConfigOnlyToOperatorsWhenAvailable() {
        ServerModHelpEntry entry = new ServerModHelpEntry("contract_notice", "Contract Tools", "Description", "/contract help", "/contract config");
        ServerModHelpRegistry.register(entry);
        try {
            String player = ServerModHelpRegistry.firstJoinMessage(false).getString();
            String operator = ServerModHelpRegistry.firstJoinMessage(true).getString();
            assertTrue(player.contains("Contract Tools"));
            assertTrue(player.contains("/servermods help"));
            assertFalse(player.contains("/servermods config"));
            assertTrue(operator.contains("/servermods config"));
        } finally {
            ServerModHelpRegistry.unregister(entry);
        }
    }

    @Test
    void entriesNormalizeTextAndRejectUnusableCommandsAndIds() {
        ServerModHelpEntry entry = ServerModHelpEntry.playerOnly(" contract_normalized ", " Tools ", " Description ", " /tools help ");
        assertEquals("contract_normalized", entry.modId());
        assertEquals("Tools", entry.displayName());
        assertEquals("Description", entry.description());
        assertEquals("/tools help", entry.helpCommand());
        assertNull(entry.adminConfigCommand());
        assertThrows(IllegalArgumentException.class, () -> ServerModHelpEntry.playerOnly("Invalid ID", "Tools", "Description", "/tools"));
        assertThrows(IllegalArgumentException.class, () -> ServerModHelpEntry.playerOnly("valid", " ", "Description", "/tools"));
        assertThrows(IllegalArgumentException.class, () -> ServerModHelpEntry.playerOnly("valid", "Tools", "Description", "tools"));
        assertThrows(IllegalArgumentException.class, () -> new ServerModHelpEntry("valid", "Tools", "Description", "/tools", "config"));
    }
}
