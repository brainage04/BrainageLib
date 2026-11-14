package io.github.brainage04.brainagelib.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OwnerAccessListTest {
    private static final UUID OWNER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID GUEST = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final UUID OTHER = UUID.fromString("16fd2706-8baf-433b-82eb-8c7fada847da");

    private final AtomicInteger changes = new AtomicInteger();
    private final OwnerAccessList<UUID> list = OwnerAccessList.byOwner(changes::incrementAndGet);

    private record Home(UUID owner, String name) {}

    @Test
    void theOwnerIsAlwaysAllowedAndOthersOnlyOnceGranted() {
        assertTrue(list.allows(OWNER, OWNER));
        assertFalse(list.allows(OWNER, GUEST));

        assertEquals(1, list.grant(OWNER, List.of(GUEST)));

        assertTrue(list.allows(OWNER, GUEST));
        assertFalse(list.allows(OWNER, OTHER));
        assertFalse(list.allows(GUEST, OWNER), "A grant goes one way");
    }

    @Test
    void grantCountsOnlyNewGuestsAndSkipsTheOwner() {
        assertEquals(2, list.grant(OWNER, List.of(GUEST, OWNER, OTHER)));
        assertEquals(1, changes.get());

        assertEquals(0, list.grant(OWNER, List.of(GUEST, OWNER)));
        assertEquals(
                1, changes.get(), "A grant that changes nothing does not mark the data changed");
        assertEquals(Set.of(GUEST, OTHER), list.guests(OWNER));
    }

    @Test
    void grantingOnlyTheOwnerStoresNothing() {
        assertEquals(0, list.grant(OWNER, List.of(OWNER)));

        assertEquals(Map.of(), list.snapshot(), "No owner is saved with an empty guest list");
        assertEquals(0, changes.get());
    }

    @Test
    void revokeCountsRemovedGuestsAndDropsAnEmptyOwner() {
        list.grant(OWNER, List.of(GUEST, OTHER));
        changes.set(0);

        assertEquals(1, list.revoke(OWNER, List.of(GUEST, OWNER)));
        assertEquals(1, changes.get());
        assertEquals(Set.of(OTHER), list.guests(OWNER));

        assertEquals(1, list.revoke(OWNER, List.of(OTHER)));
        assertEquals(Map.of(), list.snapshot());

        assertEquals(0, list.revoke(OWNER, List.of(OTHER)));
        assertEquals(0, list.revoke(GUEST, List.of(OWNER)));
        assertEquals(2, changes.get(), "Revokes that change nothing do not mark the data changed");
        assertTrue(list.allows(OWNER, OWNER), "Revoking never removes the owner");
    }

    @Test
    void keysNamingTheirOwnerKeepEachHomeApart() {
        OwnerAccessList<Home> homes = OwnerAccessList.create(Home::owner, changes::incrementAndGet);
        Home base = new Home(OWNER, "base");
        Home farm = new Home(OWNER, "farm");

        assertEquals(0, homes.grant(base, List.of(OWNER)));
        assertEquals(1, homes.grant(base, List.of(GUEST)));

        assertTrue(homes.allows(base, GUEST));
        assertFalse(homes.allows(farm, GUEST));
        assertTrue(homes.allows(farm, OWNER));
        assertFalse(homes.allows(new Home(GUEST, "base"), OWNER));
    }

    @Test
    void snapshotAndLoadRoundTripWithoutMarkingChanges() {
        list.grant(OWNER, List.of(GUEST));
        list.grant(GUEST, List.of(OTHER));
        changes.set(0);
        Map<UUID, Set<UUID>> saved = list.snapshot();

        OwnerAccessList<UUID> loaded = OwnerAccessList.byOwner(changes::incrementAndGet);
        loaded.load(saved);

        assertEquals(saved, loaded.snapshot());
        assertTrue(loaded.allows(GUEST, OTHER));
        assertEquals(0, changes.get());
    }

    @Test
    void loadLeavesOutOwnersAsTheirOwnGuestsAndEmptyLists() {
        Map<UUID, List<UUID>> saved = new HashMap<>();
        saved.put(OWNER, List.of(OWNER, GUEST));
        saved.put(GUEST, List.of(GUEST));
        saved.put(OTHER, List.of());

        list.grant(OTHER, List.of(OWNER));
        list.load(saved);

        assertEquals(Map.of(OWNER, Set.of(GUEST)), list.snapshot(), "Loading replaces every entry");
    }

    @Test
    void snapshotsAndGuestsAreCopies() {
        list.grant(OWNER, List.of(GUEST));
        Map<UUID, Set<UUID>> snapshot = list.snapshot();
        Set<UUID> guests = list.guests(OWNER);

        list.grant(OWNER, List.of(OTHER));

        assertEquals(Set.of(GUEST), snapshot.get(OWNER));
        assertEquals(Set.of(GUEST), guests);
        assertThrows(UnsupportedOperationException.class, () -> guests.add(OTHER));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(OWNER));
    }
}
