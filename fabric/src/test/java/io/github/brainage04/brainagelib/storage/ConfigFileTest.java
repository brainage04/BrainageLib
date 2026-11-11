package io.github.brainage04.brainagelib.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Settings DEFAULTS = new Settings(1, true);
    @TempDir Path directory;

    private ConfigFile<Settings> file() {
        return ConfigFile.builder(
                        directory.resolve("nested/config.json"), GSON, Settings.class, DEFAULTS)
                .decoder(
                        document -> {
                            Settings settings = GSON.fromJson(document, Settings.class);
                            if (settings.version != 1)
                                throw new JsonParseException(
                                        "Unsupported version " + settings.version);
                            return settings;
                        })
                .description("test configuration")
                .build();
    }

    @Test
    void missingFileIsCreatedFromTheDefaultDocument() throws IOException {
        ConfigFile<Settings> file = file();
        assertInstanceOf(JsonFile.LoadResult.Missing.class, file.loadOrCreate());
        assertSettings(1, true, file.value());
        assertEquals(
                "{\n  \"version\": 1,\n  \"enabled\": true\n}",
                Files.readString(directory.resolve("nested/config.json")));
    }

    @Test
    void loadingReplacesTheValue() throws IOException {
        write("{\"version\": 1, \"enabled\": false}");
        ConfigFile<Settings> file = file();
        assertInstanceOf(JsonFile.LoadResult.Loaded.class, file.loadOrCreate());
        assertSettings(1, false, file.value());
    }

    @Test
    void failedLoadKeepsTheValueAndTheFile() throws IOException {
        write("{\"version\": 2, \"enabled\": false}");
        ConfigFile<Settings> file = file();
        assertTrue(file.loadOrCreate().failed());
        assertSettings(1, true, file.value());
        assertFalse(file.save(), "A file that failed to load must not be replaced");
        assertEquals(
                "{\"version\": 2, \"enabled\": false}",
                Files.readString(directory.resolve("nested/config.json")));
    }

    @Test
    void reloadReplacesTheValueWithoutCreatingAMissingFile() throws IOException {
        Path path = directory.resolve("nested/config.json");
        ConfigFile<Settings> file = file();
        file.loadOrCreate();
        Files.delete(path);
        assertFalse(file.reload());
        assertFalse(Files.exists(path), "A reload must not recreate a deleted file");
        assertSettings(1, true, file.value());
    }

    @Test
    void saveWritesTheValueAndReloadReadsItBack() throws IOException {
        write("{\"version\": 1, \"enabled\": true}");
        ConfigFile<Settings> file = file();
        file.loadOrCreate();
        file.value().enabled = false;
        assertTrue(file.save());
        assertTrue(file.reload());
        assertSettings(1, false, file.value());
    }

    private void write(String document) throws IOException {
        Path path = directory.resolve("nested/config.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, document);
    }

    private static void assertSettings(int version, boolean enabled, Settings settings) {
        assertEquals(version, settings.version);
        assertEquals(enabled, settings.enabled);
    }

    private static final class Settings {
        private int version;
        private boolean enabled;

        private Settings() {}

        private Settings(int version, boolean enabled) {
            this.version = version;
            this.enabled = enabled;
        }
    }
}
