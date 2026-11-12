package io.github.brainage04.brainagelib.storage;

import com.google.gson.Gson;
import io.github.brainage04.brainagelib.BrainageLib;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * One JSON config document with a default value and the value currently in effect.
 *
 * <p>{@link #loadOrCreate} replaces the value when the document loads and writes the default
 * document when the file is missing; {@link #reload} replaces it only when the document loads, so a
 * deleted file stays deleted. A file that failed to load keeps the value and is never overwritten
 * (see {@link JsonFile}); changes then apply for the session only. {@link #save} writes nothing
 * before the first load, so it cannot replace an existing file with the default.
 *
 * <p>The value is the whole document: a missing file is created by saving the default value, so a
 * config is a type that Gson reads and writes as the file's content. Whenever the default becomes
 * the value in effect, the value is a fresh copy of it made by writing and reading it with the
 * Gson, so changing the value never changes the default. Saved files end with a line break.
 * Instances are thread-safe.
 */
public final class ConfigFile<T> {
    private final JsonFile<T> file;
    private final Gson gson;
    private final Class<T> type;
    private final T defaultValue;
    private final Logger logger;
    private T value;

    private ConfigFile(Builder<T> builder) {
        JsonFile.Builder<T> fileBuilder =
                JsonFile.builder(builder.path, builder.gson, builder.type);
        if (builder.decoder != null) fileBuilder.decoder(builder.decoder);
        if (builder.description != null) fileBuilder.description(builder.description);
        this.file = fileBuilder.logger(builder.logger).trailingNewline().build();
        this.gson = builder.gson;
        this.type = builder.type;
        this.defaultValue = builder.defaultValue;
        this.logger = builder.logger;
        this.value = copyOfDefault();
    }

    /**
     * @param path the file to read and write
     * @param gson the Gson used for the document
     * @param type the value type, which is the whole document
     * @param defaultValue the document a missing file is created with; a copy of it is the value in
     *     effect before a load
     */
    public static <T> Builder<T> builder(Path path, Gson gson, Class<T> type, T defaultValue) {
        return new Builder<>(path, gson, type, defaultValue);
    }

    /** The value in effect: the loaded one, or a copy of the default. */
    public synchronized T value() {
        return value;
    }

    /**
     * Loads the document, replacing the value when it loads, and creates it from the default value
     * when it is missing. The default write's failure is logged, not reported.
     */
    public synchronized JsonFile.LoadResult<T> loadOrCreate() {
        JsonFile.LoadResult<T> result = file.load();
        switch (result) {
            case JsonFile.LoadResult.Loaded<T>(T loaded) -> value = loaded;
            case JsonFile.LoadResult.Missing<T> missing -> {
                value = copyOfDefault();
                file.save(value);
            }
            default -> {}
        }
        return result;
    }

    /** Loads the document, replacing the value when it loads. */
    public synchronized boolean reload() {
        if (file.load() instanceof JsonFile.LoadResult.Loaded<T>(T loaded)) {
            value = loaded;
            return true;
        }
        return false;
    }

    /**
     * Writes the value; the result is {@link JsonFile#save}'s. Before the first {@link
     * #loadOrCreate} or {@link #reload} it writes nothing and returns false.
     */
    public synchronized boolean save() {
        if (file.lastLoad() == null) {
            logger.warn(
                    "Not saving {} because it was never loaded; the file is kept unchanged",
                    file.path());
            return false;
        }
        return file.save(value).saved();
    }

    private T copyOfDefault() {
        return gson.fromJson(gson.toJsonTree(defaultValue, type), type);
    }

    public static final class Builder<T> {
        private final Path path;
        private final Gson gson;
        private final Class<T> type;
        private final T defaultValue;
        private JsonFile.Decoder<T> decoder;
        private String description;
        private Logger logger = BrainageLib.LOGGER;

        private Builder(Path path, Gson gson, Class<T> type, T defaultValue) {
            this.path = Objects.requireNonNull(path, "path");
            this.gson = Objects.requireNonNull(gson, "gson");
            this.type = Objects.requireNonNull(type, "type");
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        }

        /** Replaces plain Gson decoding, for validation, normalisation or migration. */
        public Builder<T> decoder(JsonFile.Decoder<T> decoder) {
            this.decoder = Objects.requireNonNull(decoder, "decoder");
            return this;
        }

        /** Names the file in log messages, for example "Example configuration". */
        public Builder<T> description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder<T> logger(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public ConfigFile<T> build() {
            return new ConfigFile<>(this);
        }
    }
}
