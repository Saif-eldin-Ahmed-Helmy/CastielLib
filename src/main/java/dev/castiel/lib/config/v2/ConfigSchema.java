package dev.castiel.lib.config.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Immutable ordered schema for a flat set of typed configuration keys. */
public final class ConfigSchema {
    private final List<ConfigKey<?>> keys;

    private ConfigSchema(List<ConfigKey<?>> keys) {
        List<ConfigKey<?>> copy = new ArrayList<ConfigKey<?>>(keys.size());
        Set<String> paths = new HashSet<String>();
        for (ConfigKey<?> key : keys) {
            ConfigKey<?> checked = Objects.requireNonNull(key, "keys element");
            if (!paths.add(checked.path())) {
                throw new IllegalArgumentException("duplicate configuration path: " + checked.path());
            }
            copy.add(checked);
        }
        this.keys = Collections.unmodifiableList(copy);
    }

    /**
     * Creates an immutable schema retaining the exact key instances in order.
     *
     * @param keys declared keys
     * @return immutable schema
     */
    public static ConfigSchema of(List<ConfigKey<?>> keys) {
        return new ConfigSchema(new ArrayList<ConfigKey<?>>(Objects.requireNonNull(keys, "keys")));
    }

    /** @return declared keys in declaration order */
    public List<ConfigKey<?>> keys() {
        return keys;
    }

    /**
     * Resolves supplied flat values atomically into a readable snapshot.
     *
     * @param source logical configuration source label
     * @param suppliedValues caller-owned flat values
     * @return valid snapshot or an invalid fail-closed snapshot
     */
    public ConfigSnapshot resolve(String source, Map<String, ?> suppliedValues) {
        String checkedSource = requireLogicalSource(source);
        Map<String, ?> supplied = Objects.requireNonNull(suppliedValues, "suppliedValues");

        Set<String> declaredPaths = new HashSet<String>();
        for (ConfigKey<?> key : keys) {
            declaredPaths.add(key.path());
        }
        for (String path : supplied.keySet()) {
            requireMapKey(path);
        }

        List<ConfigValidationIssue> issues = new ArrayList<ConfigValidationIssue>();
        IdentityHashMap<ConfigKey<?>, Object> values = new IdentityHashMap<ConfigKey<?>, Object>();
        IdentityHashMap<ConfigKey<?>, Boolean> defaults = new IdentityHashMap<ConfigKey<?>, Boolean>();

        for (ConfigKey<?> key : keys) {
            boolean present = supplied.containsKey(key.path());
            ResolvedConfigValue<?> resolved = key.resolve(checkedSource, present, supplied.get(key.path()));
            issues.addAll(resolved.report().issues());
            if (resolved.isValid()) {
                values.put(key, resolved.value());
                defaults.put(key, Boolean.valueOf(resolved.usedDefault()));
            }
        }

        TreeSet<String> unknownPaths = new TreeSet<String>();
        for (String path : supplied.keySet()) {
            if (!declaredPaths.contains(path)) {
                unknownPaths.add(path);
            }
        }
        for (String path : unknownPaths) {
            issues.add(new ConfigValidationIssue(
                    "config.key.unknown", ValidationSeverity.WARNING, checkedSource, path,
                    "<not-read>", "a declared configuration key", "setting is ignored",
                    "remove it or correct its spelling"));
        }

        ConfigValidationReport report = ConfigValidationReport.of(issues);
        if (report.hasErrors()) {
            return new ConfigSnapshot(keys, report,
                    new IdentityHashMap<ConfigKey<?>, Object>(),
                    new IdentityHashMap<ConfigKey<?>, Boolean>());
        }
        return new ConfigSnapshot(keys, report, values, defaults);
    }

    private static void requireMapKey(String path) {
        Objects.requireNonNull(path, "suppliedValues key");
        if (path.trim().isEmpty()) {
            throw new IllegalArgumentException("suppliedValues keys must not be blank");
        }
    }

    private static String requireLogicalSource(String value) {
        Objects.requireNonNull(value, "source");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (isAbsolutePath(value)) {
            throw new IllegalArgumentException("source must be a logical label, not an absolute host path");
        }
        return value;
    }

    private static boolean isAbsolutePath(String value) {
        if (value.startsWith("/") || value.startsWith("\\")) {
            return true;
        }
        return value.length() >= 3
                && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':'
                && (value.charAt(2) == '/' || value.charAt(2) == '\\');
    }
}
