package io.github.brainage04.brainagelib.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.brainage04.brainagelib.fabric.FabricServerPlatform;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServerPlatformsTest {
    @Test
    void theFabricJarProvidesTheFabricPlatform() {
        ServerPlatform platform = ServerPlatform.get();
        assertInstanceOf(FabricServerPlatform.class, platform);
        assertEquals("Fabric", platform.loaderName());
        assertSame(platform, ServerPlatform.get(), "The platform is found once and reused");
    }

    @Test
    void theOnlyPlatformFoundIsUsed() {
        ServerPlatform platform = new FabricServerPlatform();
        assertSame(platform, ServerPlatforms.single(List.of(platform)));
    }

    @Test
    void noPlatformNamesTheMissingJar() {
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> ServerPlatforms.single(List.of()));
        assertTrue(failure.getMessage().contains("found 0"), failure.getMessage());
        assertTrue(failure.getMessage().contains("BrainageLib jar for this loader"));
    }

    @Test
    void twoPlatformsAreRefused() {
        IllegalStateException failure =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                ServerPlatforms.single(
                                        List.of(
                                                new FabricServerPlatform(),
                                                new FabricServerPlatform())));
        assertTrue(failure.getMessage().contains("found 2"), failure.getMessage());
    }
}
