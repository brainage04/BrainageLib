package io.github.brainage04.brainagelib.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFilesTest {
    @TempDir Path directory;

    @Test void writesThroughASameDirectoryTemporaryFileAndRemovesIt() throws IOException {
        Path target = directory.resolve("nested/state.json");
        AtomicFiles.writeString(target, "first");
        AtomicFiles.writeString(target, "second");
        assertEquals("second", Files.readString(target));
        assertEquals(target.resolveSibling("state.json.tmp"), AtomicFiles.stagingPath(target));
        assertFalse(Files.exists(AtomicFiles.stagingPath(target)));
    }

    @Test void failedWriteKeepsTheTargetAndRemovesTheTemporaryFile() throws IOException {
        Path target = directory.resolve("state.json");
        Files.writeString(target, "original");
        IOException failure = assertThrows(IOException.class, () -> AtomicFiles.write(target, output -> {
            output.write("partial".getBytes(StandardCharsets.UTF_8));
            throw new IOException("disk full");
        }));
        assertEquals("disk full", failure.getMessage());
        assertEquals("original", Files.readString(target));
        assertFalse(Files.exists(AtomicFiles.stagingPath(target)));
    }

    @Test void fallsBackToAPlainMoveWithoutAtomicMoves() throws IOException {
        Path staged = directory.resolve("state.json.tmp");
        Path target = directory.resolve("state.json");
        Files.writeString(staged, "new");
        Files.writeString(target, "old");
        List<List<StandardCopyOption>> attempts = new ArrayList<>();
        AtomicFiles.replace(staged, target, (source, destination, options) -> {
            attempts.add(List.of(options));
            if (List.of(options).contains(StandardCopyOption.ATOMIC_MOVE)) {
                throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "not supported");
            }
            Files.move(source, destination, options);
        });
        assertEquals(List.of(List.of(StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE),
                List.of(StandardCopyOption.REPLACE_EXISTING)), attempts);
        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(staged));
    }
}
