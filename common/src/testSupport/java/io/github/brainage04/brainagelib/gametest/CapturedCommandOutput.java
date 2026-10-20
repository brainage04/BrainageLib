package io.github.brainage04.brainagelib.gametest;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * A command source that keeps every message sent to it, for running commands as a console-like source.
 * Use {@link #source} for a server source (all permissions) that reports to this output.
 */
public final class CapturedCommandOutput implements CommandSource {
    private final List<Component> messages = new ArrayList<>();

    /** The server's own command source, reporting success and failure messages here instead of the console. */
    public CommandSourceStack source(MinecraftServer server) {
        return server.createCommandSourceStack().withSource(this);
    }

    @Override
    public synchronized void sendSystemMessage(Component message) {
        messages.add(message);
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    /** Every message received so far, oldest first. */
    public synchronized List<Component> messages() {
        return List.copyOf(messages);
    }

    /** The plain text of {@link #messages()}. */
    public synchronized List<String> messageTexts() {
        return messages.stream().map(Component::getString).toList();
    }

    /** The plain text of every message, one per line. */
    public synchronized String text() {
        return String.join("\n", messageTexts());
    }

    public synchronized void clear() {
        messages.clear();
    }
}
