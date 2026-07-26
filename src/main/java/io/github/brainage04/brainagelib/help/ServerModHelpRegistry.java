package io.github.brainage04.brainagelib.help;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ServerModHelpRegistry {
    private static final Map<String, ServerModHelpEntry> ENTRIES = new LinkedHashMap<>();

    private ServerModHelpRegistry() {
    }

    public static synchronized void register(ServerModHelpEntry entry) {
        Objects.requireNonNull(entry, "entry");
        ServerModHelpEntry previous = ENTRIES.putIfAbsent(entry.modId(), entry);
        if (previous != null && !previous.equals(entry)) {
            throw new IllegalStateException("Help is already registered for mod " + entry.modId());
        }
    }

    /**
     * Removes a previously registered entry when it is no longer applicable.
     *
     * @return {@code true} when this exact entry was registered and removed
     */
    public static synchronized boolean unregister(ServerModHelpEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return ENTRIES.remove(entry.modId(), entry);
    }

    public static synchronized List<ServerModHelpEntry> entries() {
        List<ServerModHelpEntry> entries = new ArrayList<>(ENTRIES.values());
        entries.sort(Comparator.comparing(ServerModHelpEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    public static int showHelp(CommandSourceStack source) {
        List<ServerModHelpEntry> entries = entries();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> prefixed(Component.literal("No server mods have registered help."), ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() -> prefixed(Component.literal("Installed server mods:"), ChatFormatting.YELLOW), false);
        for (ServerModHelpEntry entry : entries) {
            source.sendSuccess(() -> Component.literal(" - ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entry.displayName()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": " + entry.description() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(entry.helpCommand()).withStyle(ChatFormatting.YELLOW)), false);
        }
        return 1;
    }

    public static int showAdminConfig(CommandSourceStack source) {
        List<ServerModHelpEntry> configurable = entries().stream()
                .filter(entry -> entry.adminConfigCommand() != null)
                .toList();
        if (configurable.isEmpty()) {
            source.sendSuccess(() -> prefixed(Component.literal("No installed server mods expose an admin config command."), ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() -> prefixed(Component.literal("Operator configuration commands:"), ChatFormatting.YELLOW), false);
        for (ServerModHelpEntry entry : configurable) {
            source.sendSuccess(() -> Component.literal(" - ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entry.displayName() + ": ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(entry.adminConfigCommand()).withStyle(ChatFormatting.YELLOW)), false);
        }
        return 1;
    }

    public static Component firstJoinMessage(boolean operator) {
        List<ServerModHelpEntry> entries = entries();
        MutableComponent message = Component.literal("[Server Mods] ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("This server uses ").withStyle(ChatFormatting.YELLOW));

        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                message.append(Component.literal(index == entries.size() - 1 ? " and " : ", ")
                        .withStyle(ChatFormatting.YELLOW));
            }
            message.append(Component.literal(entries.get(index).displayName()).withStyle(ChatFormatting.AQUA));
        }

        message.append(Component.literal(". Run /servermods help to learn more.").withStyle(ChatFormatting.YELLOW));
        if (operator && entries.stream().anyMatch(entry -> entry.adminConfigCommand() != null)) {
            message.append(Component.literal(" Operators can run /servermods config.").withStyle(ChatFormatting.GOLD));
        }
        return message;
    }

    private static MutableComponent prefixed(Component body, ChatFormatting formatting) {
        return Component.literal("[Server Mods] ")
                .withStyle(ChatFormatting.GRAY)
                .append(body.copy().withStyle(formatting));
    }
}
