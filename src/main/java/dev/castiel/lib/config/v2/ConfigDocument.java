package dev.castiel.lib.config.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, format-neutral configuration document with a logical source and
 * explicit schema version.
 *
 * <p>The document copies its mapping structure, retaining encounter order.
 * Values are intentionally retained by reference: adapters must treat value
 * objects as immutable, because this class does not pretend to deep-copy
 * unknown formats.</p>
 */
public final class ConfigDocument {
    private final String source;
    private final int schemaVersion;
    private final Map<String, Object> values;

    private ConfigDocument(String source, int schemaVersion, Map<String, ?> values) {
        this.source = requireLogicalSource(source);
        this.schemaVersion = requireVersion(schemaVersion);

        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ?> entry : Objects.requireNonNull(values, "values").entrySet()) {
            String path = requirePath(entry.getKey());
            copy.put(path, entry.getValue());
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    /**
     * Creates an empty document at the supplied schema version.
     *
     * @param source logical source label, not an absolute host path
     * @param schemaVersion current non-negative schema version
     * @return empty immutable document
     */
    public static ConfigDocument fresh(String source, int schemaVersion) {
        return new ConfigDocument(source, schemaVersion,
                Collections.<String, Object>emptyMap());
    }

    /**
     * Creates a document by copying raw entries in map encounter order.
     *
     * @param source logical source label, not an absolute host path
     * @param schemaVersion current non-negative schema version
     * @param values raw entries, including unknown entries and explicit nulls
     * @return immutable document
     */
    public static ConfigDocument of(String source, int schemaVersion,
                                     Map<String, ?> values) {
        return new ConfigDocument(source, schemaVersion, values);
    }

    /** @return logical source label */
    public String source() {
        return source;
    }

    /** @return explicit non-negative schema version */
    public int schemaVersion() {
        return schemaVersion;
    }

    /**
     * Returns the immutable raw mapping in encounter order.
     *
     * <p>The map structure is immutable, but arbitrary value objects are
     * retained by reference and must be treated as immutable by adapters.</p>
     *
     * @return unmodifiable raw values
     */
    public Map<String, Object> values() {
        return values;
    }

    /**
     * Tests whether a raw path is present, including when its value is null.
     *
     * @param path raw path
     * @return whether the path is present
     */
    public boolean contains(String path) {
        return values.containsKey(requirePath(path));
    }

    /**
     * Returns a raw value, or null when the path is absent or explicitly null.
     * Use {@link #contains(String)} to distinguish those cases.
     *
     * @param path raw path
     * @return raw value
     */
    public Object value(String path) {
        return values.get(requirePath(path));
    }

    /**
     * Returns a copy with one value replaced in place or appended.
     *
     * @param path raw path to replace or append
     * @param value raw value, which may be null
     * @return updated immutable document
     */
    public ConfigDocument withValue(String path, Object value) {
        String checkedPath = requirePath(path);
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(values);
        copy.put(checkedPath, value);
        return new ConfigDocument(source, schemaVersion, copy);
    }

    /**
     * Returns a copy without an existing raw value.
     *
     * @param path existing raw path
     * @return updated immutable document
     * @throws IllegalArgumentException if the path is absent
     */
    public ConfigDocument withoutValue(String path) {
        String checkedPath = requirePath(path);
        if (!values.containsKey(checkedPath)) {
            throw new IllegalArgumentException("path is not present: " + checkedPath);
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(values);
        copy.remove(checkedPath);
        return new ConfigDocument(source, schemaVersion, copy);
    }

    /**
     * Returns a copy advanced by exactly one schema version.
     *
     * @param nextSchemaVersion next schema version
     * @return updated immutable document
     * @throws IllegalArgumentException if the version is not exactly current + 1
     */
    public ConfigDocument advanceTo(int nextSchemaVersion) {
        requireVersion(nextSchemaVersion);
        if (nextSchemaVersion != schemaVersion + 1) {
            throw new IllegalArgumentException("schema version must advance by exactly one");
        }
        return new ConfigDocument(source, nextSchemaVersion, values);
    }

    /**
     * Resolves this document through the supplied typed schema without
     * changing its raw entries.
     *
     * @param schema schema to resolve
     * @return immutable schema snapshot
     */
    public ConfigSnapshot resolve(ConfigSchema schema) {
        return Objects.requireNonNull(schema, "schema").resolve(source, values);
    }

    /**
     * Returns structural information only; raw keys and values are excluded.
     *
     * @return safe structural description
     */
    @Override
    public String toString() {
        return "ConfigDocument{" +
                "source='" + source + '\'' +
                ", schemaVersion=" + schemaVersion +
                ", keyCount=" + values.size() +
                '}';
    }

    private static String requirePath(String path) {
        Objects.requireNonNull(path, "path");
        if (path.trim().isEmpty()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return path;
    }

    private static int requireVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("schemaVersion must not be negative");
        }
        return version;
    }

    private static String requireLogicalSource(String source) {
        Objects.requireNonNull(source, "source");
        if (source.trim().isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (isAbsolutePath(source)) {
            throw new IllegalArgumentException(
                    "source must be a logical label, not an absolute host path");
        }
        return source;
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
