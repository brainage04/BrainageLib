package io.github.brainage04.brainagelib.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Replaces a file so that readers see either the old or the new content, never a partly written file.
 *
 * <p>The new content is written to {@code <name>.tmp} in the target's directory and then moved over the target
 * atomically. File systems without atomic moves fall back to a plain replacing move. The temporary file is deleted
 * when writing or moving fails; the target is then left as it was.
 */
public final class AtomicFiles {
    private static final String TEMPORARY_SUFFIX = ".tmp";

    private AtomicFiles() {}

    /** Writes content produced by {@code content} to {@code target}, replacing it atomically. */
    public static void write(Path target, IOConsumer<? super OutputStream> content) throws IOException {
        Objects.requireNonNull(content, "content");
        Path staged = stagingPath(target);
        Path directory = staged.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        try {
            try (OutputStream output = Files.newOutputStream(staged)) {
                content.accept(output);
            }
            replace(staged, target);
        } catch (IOException | RuntimeException exception) {
            deleteAfterFailure(staged, exception);
            throw exception;
        }
    }

    /** Writes UTF-8 text produced by {@code content} to {@code target}, replacing it atomically. */
    public static void writeText(Path target, IOConsumer<? super Writer> content) throws IOException {
        Objects.requireNonNull(content, "content");
        write(target, output -> {
            Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            content.accept(writer);
            writer.flush();
        });
    }

    /** Writes {@code text} as UTF-8 to {@code target}, replacing it atomically. */
    public static void writeString(Path target, String text) throws IOException {
        Objects.requireNonNull(text, "text");
        writeText(target, writer -> writer.write(text));
    }

    /**
     * Moves an already written {@code staged} file over {@code target}, atomically where the file system allows it.
     * {@code staged} should be in the target's directory, since only same-directory moves are reliably atomic.
     */
    public static void replace(Path staged, Path target) throws IOException {
        replace(staged, target, Files::move);
    }

    /** The temporary file {@link #write} uses for {@code target}. */
    public static Path stagingPath(Path target) {
        Path fileName = Objects.requireNonNull(target, "target").getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("target must name a file: " + target);
        }
        return target.resolveSibling(fileName + TEMPORARY_SUFFIX);
    }

    static void replace(Path staged, Path target, Mover mover) throws IOException {
        Objects.requireNonNull(staged, "staged");
        Objects.requireNonNull(target, "target");
        try {
            mover.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            mover.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteAfterFailure(Path staged, Exception failure) {
        try {
            Files.deleteIfExists(staged);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    /** Writes content to a stream; may throw {@link IOException}. */
    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T value) throws IOException;
    }

    /** {@link Files#move}, replaceable in tests to simulate file systems without atomic moves. */
    @FunctionalInterface
    interface Mover {
        void move(Path source, Path target, StandardCopyOption... options) throws IOException;
    }
}
