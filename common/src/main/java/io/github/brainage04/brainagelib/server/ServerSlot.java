package io.github.brainage04.brainagelib.server;

import org.jspecify.annotations.Nullable;

/** The one running server of {@link CurrentServer}, generic so that it can be tested alone. */
final class ServerSlot<S> {
    private volatile @Nullable S server;

    @Nullable S get() {
        return server;
    }

    void start(S starting) {
        server = starting;
    }

    void stop(S stopped) {
        if (server == stopped) {
            server = null;
        }
    }
}
