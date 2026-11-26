package io.github.brainage04.brainagelib.access;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Which players an owner lets use something of theirs: a spawn, a named home. Each entry is a key
 * that names its owner (the owner's UUID, or a record of owner and home name) and the guests the
 * owner granted. The owner is always allowed and is never stored as a guest, and a key without
 * guests is not stored.
 *
 * <p>Changes call the {@code onChange} callback (a saved data's {@code setDirty}) only when they
 * changed something. The list does not save itself: {@link #snapshot()} and {@link #load(Map)} give
 * a mod's codec the entries. Use it from the server thread.
 *
 * @param <K> the key, from which {@code ownerOf} reads the owner
 */
public final class OwnerAccessList<K> {
    private final Function<? super K, UUID> ownerOf;
    private final Runnable onChange;
    private final Map<K, Set<UUID>> guests = new HashMap<>();

    private OwnerAccessList(Function<? super K, UUID> ownerOf, Runnable onChange) {
        this.ownerOf = Objects.requireNonNull(ownerOf, "ownerOf");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /** A list keyed by the owner's UUID: one set of guests per owner. */
    public static OwnerAccessList<UUID> byOwner(Runnable onChange) {
        return new OwnerAccessList<>(Function.identity(), onChange);
    }

    /** A list keyed by {@code K}, such as a record of owner and home name. */
    public static <K> OwnerAccessList<K> create(
            Function<? super K, UUID> ownerOf, Runnable onChange) {
        return new OwnerAccessList<>(ownerOf, onChange);
    }

    /** Whether {@code player} is the key's owner or one of its guests. */
    public boolean allows(K key, UUID player) {
        return ownerOf.apply(key).equals(player)
                || guests.getOrDefault(key, Set.of()).contains(player);
    }

    /** The key's guests, without the owner, as an unmodifiable copy. */
    public Set<UUID> guests(K key) {
        return Set.copyOf(guests.getOrDefault(key, Set.of()));
    }

    /**
     * Adds {@code players} as guests of {@code key}, skipping the owner and players already there.
     *
     * @return how many guests were added
     */
    public int grant(K key, Collection<UUID> players) {
        UUID owner = ownerOf.apply(key);
        Set<UUID> keyGuests = guests.get(key);
        int added = 0;
        for (UUID player : players) {
            if (owner.equals(player)) {
                continue;
            }
            if (keyGuests == null) {
                keyGuests = new LinkedHashSet<>();
                guests.put(key, keyGuests);
            }
            if (keyGuests.add(player)) {
                added++;
            }
        }
        if (added > 0) {
            onChange.run();
        }
        return added;
    }

    /**
     * Removes {@code players} from the guests of {@code key}; the key goes once it has no guests.
     *
     * @return how many guests were removed
     */
    public int revoke(K key, Collection<UUID> players) {
        Set<UUID> keyGuests = guests.get(key);
        if (keyGuests == null) {
            return 0;
        }
        int before = keyGuests.size();
        keyGuests.removeAll(players);
        int removed = before - keyGuests.size();
        if (keyGuests.isEmpty()) {
            guests.remove(key);
        }
        if (removed > 0) {
            onChange.run();
        }
        return removed;
    }

    /** Every key with its guests, as an unmodifiable copy for a codec. */
    public Map<K, Set<UUID>> snapshot() {
        Map<K, Set<UUID>> copy = new HashMap<>();
        guests.forEach((key, keyGuests) -> copy.put(key, Set.copyOf(keyGuests)));
        return Map.copyOf(copy);
    }

    /**
     * Replaces every entry with {@code saved}, as read by a codec, without calling {@code
     * onChange}. Owners listed as their own guests and keys without guests are left out.
     */
    public void load(Map<K, ? extends Collection<UUID>> saved) {
        guests.clear();
        saved.forEach(
                (key, players) -> {
                    UUID owner = ownerOf.apply(key);
                    Set<UUID> keyGuests = new LinkedHashSet<>(players);
                    keyGuests.remove(owner);
                    if (!keyGuests.isEmpty()) {
                        guests.put(key, keyGuests);
                    }
                });
    }
}
