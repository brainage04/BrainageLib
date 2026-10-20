package io.github.brainage04.brainagelib.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.github.brainage04.brainagelib.BrainageLib;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One JSON document on disk with an explicit load result and an explicit rule for what a failed load means for
 * later saves.
 *
 * <p>With the default {@link FailedLoadPolicy#KEEP_ORIGINAL}, a file that could not be read or decoded (malformed,
 * rejected by the decoder, for example a newer schema, or unreadable) is never overwritten: {@link #save} returns
 * {@link SaveResult#KEPT_ORIGINAL} until a later {@link #load} succeeds or finds the file missing. Callers keep
 * changes in memory for the session and tell the player or console that they were not saved.
 *
 * <p>Saves go through {@link AtomicFiles}. Load and save problems are logged here; callers report them to players.
 * Instances are thread-safe.
 */
public final class JsonFile<T> {
    private final Path path;
    private final Gson gson;
    private final Type type;
    private final Decoder<T> decoder;
    private final FailedLoadPolicy failedLoadPolicy;
    private final String description;
    private final Logger logger;
    private LoadResult<T> lastLoad;

    private JsonFile(Builder<T> builder) {
        this.path = builder.path;
        this.gson = builder.gson;
        this.type = builder.type;
        this.decoder = builder.decoder != null ? builder.decoder : this::decodeWithGson;
        this.failedLoadPolicy = builder.failedLoadPolicy;
        this.description = builder.description != null ? builder.description : path.getFileName().toString();
        this.logger = builder.logger;
    }

    public static <T> Builder<T> builder(Path path, Gson gson, Class<T> type) {
        return new Builder<>(path, gson, type);
    }

    public static <T> Builder<T> builder(Path path, Gson gson, TypeToken<T> type) {
        return new Builder<>(path, gson, Objects.requireNonNull(type, "type").getType());
    }

    public Path path() {
        return path;
    }

    /**
     * Reads and decodes the file. A file that does not exist is {@link LoadResult.Missing}; a document that is not
     * JSON, is JSON {@code null}, or is rejected by the decoder is {@link LoadResult.Invalid}.
     */
    public synchronized LoadResult<T> load() {
        LoadResult<T> result;
        if (Files.notExists(path)) {
            result = new LoadResult.Missing<>();
        } else {
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonElement document = JsonParser.parseReader(reader);
                if (document.isJsonNull()) {
                    throw new JsonParseException("The document is empty");
                }
                T value = decoder.decode(document);
                if (value == null) {
                    throw new JsonParseException("The document decoded to nothing");
                }
                result = new LoadResult.Loaded<>(value);
            } catch (IOException exception) {
                result = new LoadResult.IoError<>(exception);
            } catch (JsonIOException exception) {
                // Gson wraps read failures, for example a directory where the file should be.
                result = new LoadResult.IoError<>(exception.getCause() instanceof IOException cause
                        ? cause : new IOException(exception.getMessage(), exception));
            } catch (JsonParseException exception) {
                result = new LoadResult.Invalid<>(exception);
            } catch (IllegalStateException | UnsupportedOperationException exception) {
                // Thrown by Gson's JsonElement accessors when a decoder meets the wrong shape.
                result = new LoadResult.Invalid<>(new JsonParseException(exception.getMessage(), exception));
            }
        }
        lastLoad = result;
        if (result.failed()) {
            logger.error("Failed to load {} from {}; {}", description, path,
                    failedLoadPolicy == FailedLoadPolicy.KEEP_ORIGINAL
                            ? "the file will not be overwritten and changes apply for this session only"
                            : "it will be replaced by the next save",
                    result.cause());
        }
        return result;
    }

    /** The result of the most recent {@link #load}, or null when the file was never loaded. */
    public synchronized LoadResult<T> lastLoad() {
        return lastLoad;
    }

    /** Whether {@link #save} would write the file, given the most recent load and the failed-load policy. */
    public synchronized boolean savesAllowed() {
        return failedLoadPolicy == FailedLoadPolicy.ALLOW_OVERWRITE || lastLoad == null || !lastLoad.failed();
    }

    /** Writes {@code value} with this file's Gson, unless the failed-load policy keeps the original file. */
    public synchronized SaveResult save(T value) {
        Objects.requireNonNull(value, "value");
        if (!savesAllowed()) {
            logger.warn("Not saving {} to {} because it failed to load; the file is kept unchanged", description, path);
            return SaveResult.KEPT_ORIGINAL;
        }
        try {
            AtomicFiles.writeText(path, writer -> gson.toJson(value, type, writer));
            return SaveResult.SAVED;
        } catch (IOException | RuntimeException exception) {
            logger.error("Failed to save {} to {}", description, path, exception);
            return SaveResult.FAILED;
        }
    }

    private T decodeWithGson(JsonElement document) {
        return gson.fromJson(document, type);
    }

    /** What a failed load means for later saves. */
    public enum FailedLoadPolicy {
        /** Never overwrite a file that failed to load; saves return {@link SaveResult#KEPT_ORIGINAL}. The default. */
        KEEP_ORIGINAL,
        /** Let the next save replace a file that failed to load. */
        ALLOW_OVERWRITE
    }

    public enum SaveResult {
        SAVED,
        /** Not written because the file failed to load and the policy keeps it; the change applies in memory only. */
        KEPT_ORIGINAL,
        /** Writing failed; the previous file is unchanged. */
        FAILED;

        public boolean saved() {
            return this == SAVED;
        }
    }

    /** Turns the parsed document into a value; throw {@link JsonParseException} to reject it as invalid. */
    @FunctionalInterface
    public interface Decoder<T> {
        T decode(JsonElement document) throws JsonParseException;
    }

    public sealed interface LoadResult<T> {
        /** Whether the file exists but could not be read or decoded. */
        default boolean failed() {
            return this instanceof Invalid<T> || this instanceof IoError<T>;
        }

        /** The failure, or null when the load did not fail. */
        default Exception cause() {
            return switch (this) {
                case Invalid<T> invalid -> invalid.exception();
                case IoError<T> ioError -> ioError.exception();
                default -> null;
            };
        }

        record Missing<T>() implements LoadResult<T> {}

        record Loaded<T>(T value) implements LoadResult<T> {}

        /** Not JSON, JSON {@code null}, or rejected by the decoder (for example an unsupported schema version). */
        record Invalid<T>(JsonParseException exception) implements LoadResult<T> {}

        record IoError<T>(IOException exception) implements LoadResult<T> {}
    }

    public static final class Builder<T> {
        private final Path path;
        private final Gson gson;
        private final Type type;
        private Decoder<T> decoder;
        private FailedLoadPolicy failedLoadPolicy = FailedLoadPolicy.KEEP_ORIGINAL;
        private String description;
        private Logger logger = BrainageLib.LOGGER;

        private Builder(Path path, Gson gson, Type type) {
            this.path = Objects.requireNonNull(path, "path");
            this.gson = Objects.requireNonNull(gson, "gson");
            this.type = Objects.requireNonNull(type, "type");
        }

        /** Replaces plain Gson decoding, for validation, normalisation or migration. */
        public Builder<T> decoder(Decoder<T> decoder) {
            this.decoder = Objects.requireNonNull(decoder, "decoder");
            return this;
        }

        public Builder<T> failedLoadPolicy(FailedLoadPolicy failedLoadPolicy) {
            this.failedLoadPolicy = Objects.requireNonNull(failedLoadPolicy, "failedLoadPolicy");
            return this;
        }

        /** Names the file in log messages, for example "Telekinesis player settings". Defaults to the file name. */
        public Builder<T> description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder<T> logger(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public JsonFile<T> build() {
            return new JsonFile<>(this);
        }
    }
}
