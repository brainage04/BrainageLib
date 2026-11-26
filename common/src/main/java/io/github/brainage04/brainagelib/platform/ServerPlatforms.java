package io.github.brainage04.brainagelib.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Finds the {@link ServerPlatform} BrainageLib's loader module registers as a service. */
final class ServerPlatforms {
    static final ServerPlatform INSTANCE =
            single(ServiceLoader.load(ServerPlatform.class, ServerPlatform.class.getClassLoader()));

    private ServerPlatforms() {}

    static ServerPlatform single(Iterable<ServerPlatform> found) {
        List<ServerPlatform> platforms = new ArrayList<>();
        found.forEach(platforms::add);
        if (platforms.size() != 1) {
            throw new IllegalStateException(
                    "Expected one BrainageLib server platform, found "
                            + platforms.size()
                            + ": "
                            + platforms.stream()
                                    .map(platform -> platform.getClass().getName())
                                    .toList()
                            + ". Install the BrainageLib jar for this loader.");
        }
        return platforms.getFirst();
    }
}
