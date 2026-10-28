package io.github.brainage04.brainagelib.drops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The installed {@link DropPolicy} instances, highest priority first. The first one that governs a
 * player decides for them; with none installed, or none governing, nothing is delivered.
 */
public final class DropPolicies {
    /** The priority of a mod that covers every player, such as Telekinesis. */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * The priority of a minigame that owns its players while a match runs, above every general mod.
     */
    public static final int MATCH_PRIORITY = 100;

    private record Entry(int priority, DropPolicy policy) {}

    /** Replaced, never changed in place, so the server thread reads it without locking. */
    private static volatile List<Entry> entries = List.of();

    private DropPolicies() {}

    /** Installs {@code policy}; installing one that is already installed changes nothing. */
    public static synchronized void register(int priority, DropPolicy policy) {
        if (entries.stream().anyMatch(entry -> entry.policy() == policy)) {
            return;
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(priority, policy));
        // Stable: among equal priorities the policy installed first stays first.
        next.sort(Comparator.comparingInt(Entry::priority).reversed());
        entries = List.copyOf(next);
    }

    /** Removes {@code policy} if it is installed. */
    public static synchronized void unregister(DropPolicy policy) {
        List<Entry> next = new ArrayList<>(entries);
        if (next.removeIf(entry -> entry.policy() == policy)) {
            entries = List.copyOf(next);
        }
    }

    /** The policy that decides for {@code player}, or null when none governs them. */
    public static @Nullable DropPolicy governing(ServerPlayer player) {
        for (Entry entry : entries) {
            if (entry.policy().governs(player)) {
                return entry.policy();
            }
        }
        return null;
    }

    /** Whether the drops caught for {@code player} go to them. */
    public static boolean delivers(ServerPlayer player) {
        DropPolicy policy = governing(player);
        return policy != null && policy.delivers(player);
    }

    /** Whether {@code stack} may go to {@code player}, who is delivered to. */
    public static boolean accepts(ServerPlayer player, ItemStack stack) {
        DropPolicy policy = governing(player);
        return policy != null && policy.accepts(player, stack);
    }
}
