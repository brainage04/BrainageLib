package io.github.brainage04.brainagelib.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorldPlayerStoreTest {
    private static final UUID FIRST = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    @TempDir Path directory;

    private static WorldPlayerStore<Preference> store() {
        return WorldPlayerStore.builder("players.json", Preference.class, () -> new Preference(null, true))
                .description("test player settings")
                .build();
    }

    @Test void missingFileIsCreatedEmpty() throws IOException {
        WorldPlayerStore<Preference> store = store();
        assertInstanceOf(JsonFile.LoadResult.Missing.class, store.load(directory.resolve("players.json")));
        assertEquals("{\n  \"schemaVersion\": 1,\n  \"players\": {}\n}", Files.readString(directory.resolve("players.json")));
        assertTrue(store.get(FIRST).enabled());
    }

    @Test void fileWrittenByThePreviousFormatLoadsAndSavesUnchanged() throws IOException {
        Path path = directory.resolve("players.json");
        String previous = "{\n  \"schemaVersion\": 1,\n  \"players\": {\n    \"" + FIRST
                + "\": {\n      \"lastKnownName\": \"Alex\",\n      \"enabled\": false\n    }\n  }\n}";
        Files.writeString(path, previous);
        WorldPlayerStore<Preference> store = store();
        assertInstanceOf(JsonFile.LoadResult.Loaded.class, store.load(path));
        assertEquals(new Preference("Alex", false), store.get(FIRST));
        assertEquals(JsonFile.SaveResult.SAVED, store.close());
        assertEquals(previous, Files.readString(path));
        assertFalse(store.isOpen());
    }

    @Test void invalidKeysAndNullEntriesAreSkipped() throws IOException {
        Path path = directory.resolve("players.json");
        Files.writeString(path, "{\"schemaVersion\": 1, \"players\": {\"not-a-uuid\": {\"enabled\": false}, \"" + FIRST + "\": null}}");
        WorldPlayerStore<Preference> store = store();
        assertInstanceOf(JsonFile.LoadResult.Loaded.class, store.load(path));
        assertEquals(JsonFile.SaveResult.SAVED, store.save());
        assertEquals("{\n  \"schemaVersion\": 1,\n  \"players\": {}\n}", Files.readString(path));
    }

    @Test void malformedAndNewerSchemaFilesSurviveChangesAndClose() throws IOException {
        Path path = directory.resolve("players.json");
        for (String document : new String[] {"{\"schemaVersion\": 1, \"players\": {", "{\"schemaVersion\": 2, \"players\": {}}", "{\"players\": {}}"}) {
            Files.writeString(path, document);
            WorldPlayerStore<Preference> store = store();
            assertTrue(store.load(path).failed(), document);
            store.put(FIRST, new Preference("Alex", false));
            assertFalse(store.get(FIRST).enabled(), "Changes still apply for the session");
            assertEquals(JsonFile.SaveResult.KEPT_ORIGINAL, store.save());
            assertEquals(JsonFile.SaveResult.KEPT_ORIGINAL, store.close());
            assertEquals(document, Files.readString(path));
        }
    }

    @Test void saveWithoutAWorldFails() {
        assertEquals(JsonFile.SaveResult.FAILED, store().save());
    }

    @Test void getOrCreateStoresDefaults() throws IOException {
        WorldPlayerStore<MutablePreference> store = WorldPlayerStore.builder("mutable.json", MutablePreference.class, MutablePreference::new)
                .normalizer(preference -> preference.count = Math.max(0, preference.count))
                .build();
        Path path = directory.resolve("mutable.json");
        Files.writeString(path, "{\"schemaVersion\": 1, \"players\": {\"" + FIRST + "\": {\"count\": -3}}}");
        store.load(path);
        assertEquals(0, store.get(FIRST).count, "The normalizer runs on loaded entries");
        UUID second = UUID.randomUUID();
        store.getOrCreate(second).count = 5;
        assertEquals(5, store.get(second).count);
    }

    private record Preference(String lastKnownName, boolean enabled) {}

    private static final class MutablePreference {
        private int count;
    }
}
