package io.github.brainage04.brainagelib.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A real player joined to a GameTest server through {@code PlayerList.placeNewPlayer}, with a server-side packet
 * listener on an in-memory connection. Commands go through the real chat-command packet handler and every
 * clientbound packet is kept, so tests see exactly what a client would receive.
 *
 * <p>Close it (try-with-resources) when the test ends: closing removes the player from the server, disconnects the
 * listener and releases the channel. This class is test support only and is never part of a mod's runtime JAR.
 */
public final class GameTestPlayer implements AutoCloseable {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final Connection connection;
    private final EmbeddedChannel channel;
    private final List<Component> messages = new ArrayList<>();
    private boolean closed;

    private GameTestPlayer(MinecraftServer server, ServerPlayer player, Connection connection, EmbeddedChannel channel) {
        this.server = server;
        this.player = player;
        this.connection = connection;
        this.channel = channel;
    }

    /** Joins a player with a random UUID and the given name. */
    public static GameTestPlayer join(GameTestHelper helper, String name) {
        return join(helper, new GameProfile(UUID.randomUUID(), name));
    }

    /** Joins a player with the given profile, in the GameTest's level. */
    public static GameTestPlayer join(GameTestHelper helper, GameProfile profile) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        try {
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
        } catch (RuntimeException exception) {
            channel.finishAndReleaseAll();
            throw exception;
        }
        return new GameTestPlayer(server, player, connection, channel);
    }

    public ServerPlayer player() {
        return player;
    }

    /** The server side of the player's in-memory connection. */
    public Connection connection() {
        return connection;
    }

    public EmbeddedChannel channel() {
        return channel;
    }

    /** Sets the game mode through the server, as {@code /gamemode} would. */
    public GameTestPlayer gameMode(GameType gameMode) {
        player.setGameMode(gameMode);
        return this;
    }

    /** Sends the packet a client sends once it has loaded the world, so the player can be hurt and interact. */
    public GameTestPlayer markLoaded() {
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return this;
    }

    /** Moves the player to the centre of the top of {@code relativePos} in the test structure. */
    public GameTestPlayer moveTo(GameTestHelper helper, BlockPos relativePos) {
        BlockPos position = helper.absolutePos(relativePos);
        player.snapTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        return this;
    }

    /** Runs {@code command} (without the leading slash) through the chat-command packet handler. */
    public void command(String command) {
        player.connection.handleChatCommand(new ServerboundChatCommandPacket(command));
    }

    /** Every non-overlay system chat message the player has been sent so far, oldest first. */
    public List<Component> messages() {
        drainOutbound();
        return List.copyOf(messages);
    }

    /** The plain text of {@link #messages()}. */
    public List<String> messageTexts() {
        return messages().stream().map(Component::getString).toList();
    }

    /** Whether a message with exactly this plain text was sent. */
    public boolean received(String text) {
        return messageTexts().contains(text);
    }

    /** Forgets the messages received so far. */
    public void clearMessages() {
        drainOutbound();
        messages.clear();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
                server.getPlayerList().remove(player);
            }
            player.connection.disconnect(Component.literal("GameTest finished"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private void drainOutbound() {
        channel.runPendingTasks();
        channel.flushOutbound();
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof ClientboundSystemChatPacket(Component content, boolean overlay) && !overlay) {
                messages.add(content);
            }
            ReferenceCountUtil.release(outbound);
        }
    }
}
