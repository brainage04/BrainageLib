package io.github.brainage04.brainagelib.confirmation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * "Run it again to confirm" for one action, measured in server ticks.
 *
 * <p>The first {@link #requestOrConfirm} for a key records a request; a second one while the request is still open
 * confirms and consumes it. A request made at tick {@code r} is open at tick {@code t} when {@code t - r} is at most
 * the window ({@link Boundary#INCLUSIVE}) or below it ({@link Boundary#EXCLUSIVE}); a run after it closed records a
 * new request. Use one instance per action and server, and {@link #clear} it when the server stops. Callers send
 * every message themselves. Instances are thread-safe.
 */
public final class ConfirmationWindow<K> {
    private final long windowTicks;
    private final Boundary boundary;
    private final Map<K, Long> requestedAt = new HashMap<>();

    public ConfirmationWindow(long windowTicks, Boundary boundary) {
        if (windowTicks <= 0) {
            throw new IllegalArgumentException("windowTicks must be positive: " + windowTicks);
        }
        this.windowTicks = windowTicks;
        this.boundary = Objects.requireNonNull(boundary, "boundary");
    }

    public long windowTicks() {
        return windowTicks;
    }

    /** Confirms and consumes an open request for {@code key}, or records a new request at {@code nowTick}. */
    public synchronized Outcome requestOrConfirm(K key, long nowTick) {
        Objects.requireNonNull(key, "key");
        Long requested = requestedAt.remove(key);
        if (requested != null && isOpen(requested, nowTick)) {
            return Outcome.CONFIRMED;
        }
        requestedAt.put(key, nowTick);
        return Outcome.REQUESTED;
    }

    /** Whether {@code key} has a request that is still open at {@code nowTick}. */
    public synchronized boolean isPending(K key, long nowTick) {
        Long requested = requestedAt.get(key);
        return requested != null && isOpen(requested, nowTick);
    }

    /** Drops the request for {@code key}, if any. */
    public synchronized void cancel(K key) {
        requestedAt.remove(key);
    }

    /** Drops every request, for example when the server stops. */
    public synchronized void clear() {
        requestedAt.clear();
    }

    /** Drops every request that is closed at {@code nowTick}, passing each key to {@code onExpired}. */
    public synchronized void expire(long nowTick, Consumer<? super K> onExpired) {
        Objects.requireNonNull(onExpired, "onExpired");
        Iterator<Map.Entry<K, Long>> iterator = requestedAt.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, Long> entry = iterator.next();
            if (!isOpen(entry.getValue(), nowTick)) {
                iterator.remove();
                onExpired.accept(entry.getKey());
            }
        }
    }

    private boolean isOpen(long requested, long nowTick) {
        long elapsed = nowTick - requested;
        return boundary == Boundary.INCLUSIVE ? elapsed <= windowTicks : elapsed < windowTicks;
    }

    /** Whether a run exactly {@code windowTicks} after the request still confirms. */
    public enum Boundary {
        INCLUSIVE,
        EXCLUSIVE
    }

    public enum Outcome {
        /** A request was recorded; ask the player to run the command again. */
        REQUESTED,
        /** An open request was confirmed and consumed; perform the action. */
        CONFIRMED
    }
}
