package io.github.brainage04.brainagelib.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @TempDir Path directory;

    private JsonFile<Settings> file(JsonFile.FailedLoadPolicy policy) {
        return JsonFile.builder(directory.resolve("settings.json"), GSON, Settings.class)
                .decoder(document -> {
                    Settings settings = GSON.fromJson(document, Settings.class);
                    if (settings.version != 1) throw new JsonParseException("Unsupported version " + settings.version);
                    return settings;
                })
                .failedLoadPolicy(policy)
                .build();
    }

    @Test void missingFileLoadsAsMissingAndCanBeSaved() throws IOException {
        JsonFile<Settings> file = file(JsonFile.FailedLoadPolicy.KEEP_ORIGINAL);
        assertInstanceOf(JsonFile.LoadResult.Missing.class, file.load());
        assertEquals(JsonFile.SaveResult.SAVED, file.save(new Settings(1, true)));
        assertEquals("{\n  \"version\": 1,\n  \"enabled\": true\n}", Files.readString(file.path()));
        assertEquals(new Settings(1, true), ((JsonFile.LoadResult.Loaded<Settings>) file.load()).value());
    }

    @Test void malformedAndUnsupportedFilesAreKeptByDefault() throws IOException {
        for (String document : new String[] {"{\"version\": 1, \"enabled\": ", "{\"version\": 2, \"enabled\": true}", "", "null"}) {
            Files.writeString(directory.resolve("settings.json"), document);
            JsonFile<Settings> file = file(JsonFile.FailedLoadPolicy.KEEP_ORIGINAL);
            JsonFile.LoadResult<Settings> result = file.load();
            assertInstanceOf(JsonFile.LoadResult.Invalid.class, result, document);
            assertTrue(result.failed());
            assertFalse(file.savesAllowed());
            assertEquals(JsonFile.SaveResult.KEPT_ORIGINAL, file.save(new Settings(1, false)));
            assertEquals(document, Files.readString(file.path()));
        }
    }

    @Test void unreadableFileIsAnIoErrorAndKept() throws IOException {
        Files.createDirectory(directory.resolve("settings.json"));
        JsonFile<Settings> file = file(JsonFile.FailedLoadPolicy.KEEP_ORIGINAL);
        JsonFile.LoadResult<Settings> result = file.load();
        assertInstanceOf(JsonFile.LoadResult.IoError.class, result);
        assertInstanceOf(IOException.class, result.cause());
        assertEquals(JsonFile.SaveResult.KEPT_ORIGINAL, file.save(new Settings(1, false)));
        assertTrue(Files.isDirectory(file.path()));
    }

    @Test void successfulReloadAllowsSavesAgain() throws IOException {
        Path path = directory.resolve("settings.json");
        Files.writeString(path, "{");
        JsonFile<Settings> file = file(JsonFile.FailedLoadPolicy.KEEP_ORIGINAL);
        file.load();
        Files.writeString(path, "{\"version\": 1, \"enabled\": false}");
        assertInstanceOf(JsonFile.LoadResult.Loaded.class, file.load());
        assertEquals(JsonFile.SaveResult.SAVED, file.save(new Settings(1, true)));
    }

    @Test void allowOverwritePolicyReplacesAFileThatFailedToLoad() throws IOException {
        Files.writeString(directory.resolve("settings.json"), "{");
        JsonFile<Settings> file = file(JsonFile.FailedLoadPolicy.ALLOW_OVERWRITE);
        assertTrue(file.load().failed());
        assertTrue(file.savesAllowed());
        assertEquals(JsonFile.SaveResult.SAVED, file.save(new Settings(1, true)));
        assertInstanceOf(JsonFile.LoadResult.Loaded.class, file.load());
    }

    @Test void failedWriteIsReportedAndLeavesNoTemporaryFile() throws IOException {
        Path blocker = directory.resolve("blocker");
        Files.writeString(blocker, "a file where a directory is needed");
        JsonFile<Settings> file = JsonFile.builder(blocker.resolve("settings.json"), GSON, Settings.class).build();
        assertEquals(JsonFile.SaveResult.FAILED, file.save(new Settings(1, true)));
        assertFalse(JsonFile.SaveResult.FAILED.saved());
    }

    private record Settings(int version, boolean enabled) {}
}
