package io.github.brainage04.brainagelib.server;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ServerSlotTest {
    private final ServerSlot<Object> slot = new ServerSlot<>();
    private final Object first = new Object();
    private final Object second = new Object();

    @Test
    void emptyUntilAServerStarts() {
        assertNull(slot.get());
        slot.start(first);
        assertSame(first, slot.get());
    }

    @Test
    void theServerThatEndsLeavesTheSlotEmpty() {
        slot.start(first);
        slot.stop(first);
        assertNull(slot.get());
    }

    @Test
    void anOlderServerEndingLeavesTheNewOneCurrent() {
        slot.start(first);
        slot.start(second);
        slot.stop(first);
        assertSame(second, slot.get());
        slot.stop(second);
        assertNull(slot.get());
    }
}
