package io.github.brainage04.brainagelib.gamerule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * One mod's game rules with their ids, built in common code and handed to the loader's registry
 * once.
 *
 * <p>Create the rules while the mod initializes, then register them from each loader:
 *
 * <pre>{@code
 * // Fabric, in onInitialize():
 * RULES.registerAll((id, rule) -> Registry.register(BuiltInRegistries.GAME_RULE, id, rule));
 * // NeoForge, in the mod constructor:
 * modBus.addListener((RegisterEvent event) ->
 *         event.register(Registries.GAME_RULE, helper -> RULES.registerAll(helper::register)));
 * }</pre>
 *
 * <p>{@link #registerAll} runs once. Adding a rule after it throws, so a rule created too late
 * fails at once instead of silently missing from the registry. Instances are thread-safe.
 */
public final class GameRuleSet {
    private final String namespace;
    private final Map<Identifier, GameRule<?>> rules = new LinkedHashMap<>();
    private boolean registered;

    private GameRuleSet(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Starts an empty set whose rules get ids in {@code namespace}, normally the mod id.
     *
     * @throws IllegalArgumentException if {@code namespace} is not a valid identifier namespace
     */
    public static GameRuleSet create(String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        if (!Identifier.isValidNamespace(namespace)) {
            throw new IllegalArgumentException("Invalid game rule namespace: " + namespace);
        }
        return new GameRuleSet(namespace);
    }

    /** Adds {@link GameRuleFactory#bool} under {@code <namespace>:<path>}. */
    public GameRule<Boolean> bool(String path, GameRuleCategory category, boolean defaultValue) {
        return add(path, GameRuleFactory.bool(category, defaultValue));
    }

    /**
     * Adds {@link GameRuleFactory#integer(GameRuleCategory, int, int)} under {@code
     * <namespace>:<path>}.
     */
    public GameRule<Integer> integer(
            String path, GameRuleCategory category, int defaultValue, int min) {
        return add(path, GameRuleFactory.integer(category, defaultValue, min));
    }

    /**
     * Adds {@link GameRuleFactory#integer(GameRuleCategory, int, int, int)} under {@code
     * <namespace>:<path>}.
     */
    public GameRule<Integer> integer(
            String path, GameRuleCategory category, int defaultValue, int min, int max) {
        return add(path, GameRuleFactory.integer(category, defaultValue, min, max));
    }

    /**
     * Adds a rule built elsewhere under {@code <namespace>:<path>} and returns it.
     *
     * @throws IllegalArgumentException if the path is not a valid identifier path, or the path or
     *     the rule is already in the set (the registry refuses both)
     * @throws IllegalStateException if {@link #registerAll} already ran
     */
    public synchronized <T> GameRule<T> add(String path, GameRule<T> rule) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(rule, "rule");
        if (registered) {
            throw new IllegalStateException(
                    "The "
                            + namespace
                            + " game rules are already registered; "
                            + path
                            + " came too late");
        }
        if (!Identifier.isValidPath(path)) {
            throw new IllegalArgumentException("Invalid game rule path: " + path);
        }
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        if (rules.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate game rule id: " + id);
        }
        if (rules.containsValue(rule)) {
            throw new IllegalArgumentException("The same rule cannot be added twice: " + id);
        }
        rules.put(id, rule);
        return rule;
    }

    /** The rules and their ids, in the order they were added. */
    public synchronized Map<Identifier, GameRule<?>> rules() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(rules));
    }

    /**
     * Passes every rule and its id to {@code registrar}, in the order they were added. After this,
     * the set takes no more rules.
     *
     * @throws IllegalStateException if this already ran
     */
    public void registerAll(BiConsumer<Identifier, GameRule<?>> registrar) {
        Objects.requireNonNull(registrar, "registrar");
        List<Map.Entry<Identifier, GameRule<?>>> entries;
        synchronized (this) {
            if (registered) {
                throw new IllegalStateException(
                        "The " + namespace + " game rules are already registered");
            }
            registered = true;
            entries = List.copyOf(rules.entrySet());
        }
        for (Map.Entry<Identifier, GameRule<?>> entry : entries) {
            registrar.accept(entry.getKey(), entry.getValue());
        }
    }
}
