package io.github.brainage04.brainagelib.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.github.brainage04.brainagelib.BrainageLib;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Per-world player preferences keyed by UUID, stored in one JSON file in the world folder:
 * {@code {"schemaVersion": N, "players": {"<uuid>": <preference>, ...}}}.
 *
 * <p>The owning mod keeps its preference type and its rules; this class only stores them. {@link #load} is called
 * when the server starts and writes an empty document when the file is missing; {@link #close} saves and forgets
 * everything when the server stops. A document with another schema version, or one that cannot be read, is not
 * loaded and, under the default {@link JsonFile.FailedLoadPolicy#KEEP_ORIGINAL}, never overwritten: changes then
 * apply for the session only and {@link #save} reports {@link JsonFile.SaveResult#KEPT_ORIGINAL}. Entries whose key
 * is not a UUID, or whose value is null, are skipped with a warning.
 *
 * <p>Instances are thread-safe; preferences returned by {@link #getOrCreate} are owned by the store and should only
 * be changed while holding a lock that also covers the following {@link #save}.
 */
public final class WorldPlayerStore<P> {
    private final String fileName;
    private final Class<P> preferenceType;
    private final Supplier<P> defaults;
    private final int schemaVersion;
    private final Gson gson;
    private final Consumer<P> normalizer;
    private final JsonFile.FailedLoadPolicy failedLoadPolicy;
    private final String description;
    private final Logger logger;
    private final Map<UUID, P> preferences = new HashMap<>();
    private JsonFile<Document<P>> file;

    private WorldPlayerStore(Builder<P> builder) {
        this.fileName = builder.fileName;
        this.preferenceType = builder.preferenceType;
        this.defaults = builder.defaults;
        this.schemaVersion = builder.schemaVersion;
        this.gson = builder.gson;
        this.normalizer = builder.normalizer;
        this.failedLoadPolicy = builder.failedLoadPolicy;
        this.description = builder.description != null ? builder.description : fileName;
        this.logger = builder.logger;
    }

    /**
     * @param fileName       the file in the world folder, for example {@code "telekinesis-players.json"}
     * @param preferenceType one player's preferences, serialised with Gson
     * @param defaults       preferences of a player without an entry
     */
    public static <P> Builder<P> builder(String fileName, Class<P> preferenceType, Supplier<P> defaults) {
        return new Builder<>(fileName, preferenceType, defaults);
    }

    /** Loads the file in {@code server}'s world folder. */
    public JsonFile.LoadResult<?> load(MinecraftServer server) {
        return load(server.getWorldPath(LevelResource.ROOT).resolve(fileName));
    }

    /** Loads {@code path}, replacing everything held in memory; a missing file is created empty. */
    public synchronized JsonFile.LoadResult<?> load(Path path) {
        preferences.clear();
        file = JsonFile.builder(path, gson, documentType())
                .decoder(this::decode)
                .failedLoadPolicy(failedLoadPolicy)
                .description(description)
                .logger(logger)
                .build();
        JsonFile.LoadResult<Document<P>> result = file.load();
        switch (result) {
            case JsonFile.LoadResult.Missing<Document<P>> missing -> save();
            case JsonFile.LoadResult.Loaded<Document<P>> loaded -> loaded.value().players().forEach((key, preference) -> {
                UUID id = parseUuid(key);
                if (id == null) {
                    logger.warn("Ignoring invalid player UUID in {}: {}", path, key);
                } else if (preference != null) {
                    normalizer.accept(preference);
                    preferences.put(id, preference);
                }
            });
            default -> {
            }
        }
        return result;
    }

    /** The stored preferences of {@code id}, or new defaults that are not stored. */
    public synchronized P get(UUID id) {
        P preference = preferences.get(id);
        return preference != null ? preference : defaults.get();
    }

    /** The stored preferences of {@code id}, storing new defaults first when there are none. */
    public synchronized P getOrCreate(UUID id) {
        return preferences.computeIfAbsent(id, ignored -> defaults.get());
    }

    public synchronized void put(UUID id, P preference) {
        preferences.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(preference, "preference"));
    }

    /** Whether a world's file is loaded. */
    public synchronized boolean isOpen() {
        return file != null;
    }

    /** Writes every stored preference; {@link JsonFile.SaveResult#FAILED} when no world is loaded. */
    public synchronized JsonFile.SaveResult save() {
        if (file == null) {
            return JsonFile.SaveResult.FAILED;
        }
        Map<String, P> players = new HashMap<>();
        preferences.forEach((id, preference) -> players.put(id.toString(), preference));
        return file.save(new Document<>(schemaVersion, players));
    }

    /** Saves, then forgets the world: call when the server stops. */
    public synchronized JsonFile.SaveResult close() {
        JsonFile.SaveResult result = save();
        preferences.clear();
        file = null;
        return result;
    }

    private Document<P> decode(JsonElement element) {
        if (!(element instanceof JsonObject object)) {
            throw new JsonParseException("Expected a JSON object");
        }
        JsonElement version = object.get("schemaVersion");
        if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || version.getAsInt() != schemaVersion) {
            throw new JsonParseException("Unsupported schemaVersion " + version + "; expected " + schemaVersion);
        }
        Document<P> document = gson.fromJson(object, documentType());
        if (document.players() == null) {
            throw new JsonParseException("Missing players");
        }
        return document;
    }

    @SuppressWarnings("unchecked")
    private TypeToken<Document<P>> documentType() {
        return (TypeToken<Document<P>>) TypeToken.getParameterized(Document.class, preferenceType);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** The stored document; component order is the JSON field order. */
    private record Document<P>(int schemaVersion, Map<String, P> players) {}

    public static final class Builder<P> {
        private final String fileName;
        private final Class<P> preferenceType;
        private final Supplier<P> defaults;
        private int schemaVersion = 1;
        private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        private Consumer<P> normalizer = preference -> {};
        private JsonFile.FailedLoadPolicy failedLoadPolicy = JsonFile.FailedLoadPolicy.KEEP_ORIGINAL;
        private String description;
        private Logger logger = BrainageLib.LOGGER;

        private Builder(String fileName, Class<P> preferenceType, Supplier<P> defaults) {
            this.fileName = Objects.requireNonNull(fileName, "fileName");
            this.preferenceType = Objects.requireNonNull(preferenceType, "preferenceType");
            this.defaults = Objects.requireNonNull(defaults, "defaults");
        }

        /** The only schema version that loads, and the one that is written. Defaults to 1. */
        public Builder<P> schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        /** Defaults to pretty printing without HTML escaping. */
        public Builder<P> gson(Gson gson) {
            this.gson = Objects.requireNonNull(gson, "gson");
            return this;
        }

        /** Repairs each loaded preference, for example dropping invalid entries. */
        public Builder<P> normalizer(Consumer<P> normalizer) {
            this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
            return this;
        }

        public Builder<P> failedLoadPolicy(JsonFile.FailedLoadPolicy failedLoadPolicy) {
            this.failedLoadPolicy = Objects.requireNonNull(failedLoadPolicy, "failedLoadPolicy");
            return this;
        }

        /** Names the file in log messages, for example "Telekinesis player settings". */
        public Builder<P> description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder<P> logger(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public WorldPlayerStore<P> build() {
            return new WorldPlayerStore<>(this);
        }
    }
}
