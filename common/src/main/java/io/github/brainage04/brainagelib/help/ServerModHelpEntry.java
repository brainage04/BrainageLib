package io.github.brainage04.brainagelib.help;

import java.util.Objects;
import java.util.regex.Pattern;

public record ServerModHelpEntry(String modId, String displayName, String description, String helpCommand, String adminConfigCommand) {
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("[a-z][a-z0-9_.-]*");
    public ServerModHelpEntry {
        modId = requireText(modId, "modId"); displayName = requireText(displayName, "displayName"); description = requireText(description, "description"); helpCommand = requireCommand(helpCommand, "helpCommand");
        if (!MOD_ID_PATTERN.matcher(modId).matches()) throw new IllegalArgumentException("Invalid mod id: " + modId);
        if (adminConfigCommand != null) adminConfigCommand = requireCommand(adminConfigCommand, "adminConfigCommand");
    }
    public static ServerModHelpEntry playerOnly(String modId, String displayName, String description, String helpCommand) { return new ServerModHelpEntry(modId, displayName, description, helpCommand, null); }
    private static String requireCommand(String value, String name) { String command = requireText(value, name); if (!command.startsWith("/")) throw new IllegalArgumentException(name + " must start with '/'"); return command; }
    private static String requireText(String value, String name) { Objects.requireNonNull(value, name); String normalized = value.strip(); if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank"); return normalized; }
}
