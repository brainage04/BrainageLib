package io.github.brainage04.brainagelib.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommandRateLimitTest {
    @Test
    void aSecondUseInTheSameSecondIsRefused() {
        long[] now = {0};
        CommandRateLimit limit = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        assertEquals(0, limit.refuse(player));
        assertEquals(1_000_000_000L, limit.refuse(player));
        assertEquals(0, limit.refuse(UUID.randomUUID()), "another player has their own allowance");
        now[0] = 1_000_000_000L;
        assertEquals(0, limit.refuse(player));
    }

    @Test
    void theMinuteLimitCountsTwenty() {
        long[] now = {0};
        CommandRateLimit limit = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        for (int use = 0; use < 20; use++) {
            assertEquals(0, limit.refuse(player), "use " + use);
            now[0] += 1_000_000_000L;
        }
        assertEquals(40_000_000_000L, limit.refuse(player), "the first use leaves the minute");
        // A refused use is not counted, so trying again does not push the wait further out.
        assertEquals(40_000_000_000L, limit.refuse(player));
    }

    @Test
    void theHourLimitCountsFourHundred() {
        long[] now = {0};
        CommandRateLimit limit = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        // Four seconds apart, so the minute limit (20) is never reached before the hour's 400.
        for (int use = 0; use < 400; use++) {
            assertEquals(0, limit.refuse(player), "use " + use);
            now[0] += 4_000_000_000L;
        }
        assertEquals(2_000_000_000_000L, limit.refuse(player), "the first use leaves the hour");
    }

    /**
     * A use older than the hour is forgotten, so the hour limit is a window and not a lifetime
     * cap: a player who used the command 400 times yesterday is allowed again today.
     */
    @Test
    void usesOlderThanTheHourAreForgotten() {
        long[] now = {0};
        CommandRateLimit limit = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        for (int use = 0; use < 400; use++) {
            assertEquals(0, limit.refuse(player), "use " + use);
            now[0] += 4_000_000_000L;
        }
        assertEquals(2_000_000_000_000L, limit.refuse(player), "the hour is full");
        now[0] += 2_000_000_000_000L;
        assertEquals(0, limit.refuse(player), "every use is now older than the hour");
    }

    /**
     * Two allowances are charged apart, so a command that prints a candidate list cannot spend the
     * click its own buttons need, and the list is still bounded.
     */
    @Test
    void twoAllowancesAreChargedApart() {
        long[] now = {0};
        CommandRateLimit share = new CommandRateLimit(() -> now[0]);
        CommandRateLimit listing = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        assertEquals(0, listing.refuse(player), "the list is allowed");
        assertEquals(0, share.refuse(player), "the list must not have spent the share");
        assertEquals(1_000_000_000L, listing.refuse(player), "a second list is refused");
        assertEquals(1_000_000_000L, share.refuse(player), "the share has its own second");
    }

    @Test
    void theWaitIsDescribedInWholeSeconds() {
        assertEquals("1 second", CommandRateLimit.describe(1));
        assertEquals("1 second", CommandRateLimit.describe(1_000_000_000L));
        assertEquals("2 seconds", CommandRateLimit.describe(1_500_000_000L));
    }

    /** A blank name would silently share the main allowance, so it is refused. */
    @Test
    void aBlankNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CommandRateLimit.of(null, ""));
        assertThrows(IllegalArgumentException.class, () -> CommandRateLimit.of(null, " "));
    }

    /** Clearing a server frees every name it was asked for. */
    @Test
    void clearAllFreesEveryNameOfAServer() {
        CommandRateLimit main = CommandRateLimit.of(null);
        CommandRateLimit listing = CommandRateLimit.of(null, "listing");
        UUID player = UUID.randomUUID();
        try {
            assertEquals(0, main.refuse(player));
            assertEquals(0, listing.refuse(player));
            CommandRateLimit.clearAll(null);
            assertEquals(0, main.refuse(player), "the main allowance must be freed");
            assertEquals(0, listing.refuse(player), "the named allowance must be freed");
        } finally {
            CommandRateLimit.clearAll(null);
        }
    }

    @Test
    void clearForgetsEveryUse() {
        long[] now = {0};
        CommandRateLimit limit = new CommandRateLimit(() -> now[0]);
        UUID player = UUID.randomUUID();
        assertEquals(0, limit.refuse(player));
        assertEquals(1_000_000_000L, limit.refuse(player));
        limit.clear();
        assertEquals(0, limit.refuse(player));
    }
}
