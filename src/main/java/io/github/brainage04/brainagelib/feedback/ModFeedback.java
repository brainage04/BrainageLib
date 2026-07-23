package io.github.brainage04.brainagelib.feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Objects;

public final class ModFeedback {
    private final String modName;

    private ModFeedback(String modName) {
        this.modName = requireText(modName, "modName");
    }

    public static ModFeedback create(String modName) {
        return new ModFeedback(modName);
    }

    public void neutral(ServerPlayer player, Component message) {
        player.sendSystemMessage(decorate(ChatFormatting.YELLOW, message));
    }

    public void neutral(ServerPlayer player, String message, Object... arguments) {
        neutral(player, literal(message, arguments));
    }

    public void success(ServerPlayer player, Component message) {
        player.sendSystemMessage(decorate(ChatFormatting.GREEN, message));
    }

    public void success(ServerPlayer player, String message, Object... arguments) {
        success(player, literal(message, arguments));
    }

    public void error(ServerPlayer player, Component message) {
        player.sendSystemMessage(decorate(ChatFormatting.RED, message));
    }

    public void error(ServerPlayer player, String message, Object... arguments) {
        error(player, literal(message, arguments));
    }

    public int neutral(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> decorate(ChatFormatting.YELLOW, message), false);
        return 1;
    }

    public int neutral(CommandSourceStack source, String message, Object... arguments) {
        return neutral(source, literal(message, arguments));
    }

    public int success(CommandSourceStack source, Component message, boolean broadcastToOperators) {
        source.sendSuccess(() -> decorate(ChatFormatting.GREEN, message), broadcastToOperators);
        return 1;
    }

    public int success(CommandSourceStack source, String message, boolean broadcastToOperators, Object... arguments) {
        return success(source, literal(message, arguments), broadcastToOperators);
    }

    public int failure(CommandSourceStack source, Component message) {
        source.sendFailure(decorate(ChatFormatting.RED, message));
        return 0;
    }

    public int failure(CommandSourceStack source, String message, Object... arguments) {
        return failure(source, literal(message, arguments));
    }

    public void click(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(
                SoundEvents.UI_BUTTON_CLICK,
                SoundSource.UI,
                player.getX(),
                player.getY(),
                player.getZ(),
                0.7F,
                1.0F,
                0L
        ));
    }

    public MutableComponent decorate(ChatFormatting bodyFormatting, Component message) {
        Objects.requireNonNull(bodyFormatting, "bodyFormatting");
        Objects.requireNonNull(message, "message");
        return Component.literal("[" + modName + "] ")
                .withStyle(ChatFormatting.GRAY)
                .append(message.copy().withStyle(bodyFormatting));
    }

    private static Component literal(String message, Object... arguments) {
        String template = requireText(message, "message");
        return Component.literal(arguments.length == 0 ? template : template.formatted(arguments));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
