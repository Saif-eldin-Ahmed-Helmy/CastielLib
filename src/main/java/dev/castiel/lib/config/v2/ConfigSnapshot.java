package dev.castiel.lib.config.v2;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, fail-closed result of resolving a {@link ConfigSchema}. */
public final class ConfigSnapshot {
    private final boolean valid;
    private final ConfigValidationReport report;
    private final Map<ConfigKey<?>, Boolean> registered;
    private final Map<ConfigKey<?>, Object> values;
    private final Map<ConfigKey<?>, Boolean> defaults;

    ConfigSnapshot(List<ConfigKey<?>> keys, ConfigValidationReport report,
                   Map<ConfigKey<?>, Object> values,
                   Map<ConfigKey<?>, Boolean> defaults) {
        this.report = Objects.requireNonNull(report, "report");
        this.valid = !report.hasErrors();
        this.registered = new IdentityHashMap<ConfigKey<?>, Boolean>();
        for (ConfigKey<?> key : Objects.requireNonNull(keys, "keys")) {
            this.registered.put(Objects.requireNonNull(key, "keys element"), Boolean.TRUE);
        }
        this.values = new IdentityHashMap<ConfigKey<?>, Object>(
                Objects.requireNonNull(values, "values"));
        this.defaults = new IdentityHashMap<ConfigKey<?>, Boolean>(
                Objects.requireNonNull(defaults, "defaults"));
    }

    /** @return whether every declared key resolved successfully */
    public boolean isValid() {
        return valid;
    }

    /** @return the complete immutable validation report */
    public ConfigValidationReport report() {
        return report;
    }

    /**
     * Returns the accepted value for an exact registered key instance.
     *
     * @param key registered schema key
     * @param <T> key value type
     * @return accepted or default value
     * @throws IllegalStateException if this snapshot is invalid or the key is not registered
     */
    public <T> T get(ConfigKey<T> key) {
        ensureReadable();
        ConfigKey<T> checked = requireRegistered(key);
        return checked.type().cast(values.get(checked));
    }

    /**
     * Reports whether the exact registered key used its explicit default.
     *
     * @param key registered schema key
     * @return whether the key used its default
     * @throws IllegalStateException if this snapshot is invalid or the key is not registered
     */
    public boolean usedDefault(ConfigKey<?> key) {
        ensureReadable();
        ConfigKey<?> checked = requireRegistered(key);
        return defaults.get(checked).booleanValue();
    }

    private void ensureReadable() {
        if (!valid) {
            throw new IllegalStateException("configuration snapshot is invalid");
        }
    }

    private <T> ConfigKey<T> requireRegistered(ConfigKey<T> key) {
        ConfigKey<T> checked = Objects.requireNonNull(key, "key");
        if (!registered.containsKey(checked)) {
            throw new IllegalStateException("key is not registered in this schema");
        }
        return checked;
    }
}
