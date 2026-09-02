package dev.castiel.lib.config.v2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Bounded, safe in-memory YAML boundary for the format-neutral config document. */
public final class YamlConfigDecoder {
    private static final String IMPACT = "configuration cannot be loaded";
    private static final int MAX_ALIASES = 32;
    private static final int MAX_NESTING_DEPTH = 64;
    private static final int MAX_CODE_POINTS = 1_000_000;
    private static final int MAX_SAFE_PATH_LENGTH = 128;

    public YamlConfigDecoder() { }

    /**
     * Decodes one YAML mapping without retaining parser-owned mutable objects.
     *
     * @param source logical source label
     * @param yaml YAML text
     * @param freshSchemaVersion version for blank input
     * @return successful immutable document or safe actionable failures
     */
    public ConfigDocumentLoadResult decode(String source, String yaml, int freshSchemaVersion) {
        requireCallerInputs(source, yaml, freshSchemaVersion);
        if (yaml.codePointCount(0, yaml.length()) > MAX_CODE_POINTS) {
            return failure(issue("config.yaml.limit", source, "<root>",
                    "an input within the configured size limit", "reduce the YAML document size"));
        }
        if (isBlankOrComments(yaml)) {
            return ConfigDocumentLoadResult.success(ConfigDocument.fresh(source, freshSchemaVersion));
        }

        Object root;
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setAllowRecursiveKeys(false);
            options.setMaxAliasesForCollections(MAX_ALIASES);
            options.setNestingDepthLimit(MAX_NESTING_DEPTH);
            options.setCodePointLimit(MAX_CODE_POINTS);
            Yaml parser = new Yaml(new SafeConstructor(options));
            Iterator<Object> documents = parser.loadAll(yaml).iterator();
            if (!documents.hasNext()) {
                return failure(issue("config.yaml.root", source, "<root>",
                        "a top-level mapping", "provide a top-level YAML mapping"));
            }
            root = documents.next();
            if (documents.hasNext()) {
                return failure(issue("config.yaml.syntax", source, "<root>",
                        "exactly one YAML document", "remove the additional YAML document"));
            }
        } catch (YAMLException ignored) {
            return failure(parserFailure(ignored, source));
        } catch (RuntimeException ignored) {
            return failure(issue("config.yaml.syntax", source, "<root>",
                    "valid YAML using supported safe types", "repair YAML syntax and try again"));
        }
        return decodeRoot(source, root);
    }

    private static ConfigDocumentLoadResult decodeRoot(String source, Object root) {
        if (!(root instanceof Map)) {
            return failure(issue("config.yaml.root", source, "<root>",
                    "a top-level mapping", "provide a top-level YAML mapping"));
        }
        Map<?, ?> rootMap = (Map<?, ?>) root;
        if (rootMap.isEmpty()) {
            return failure(issue("config.yaml.root", source, "<root>",
                    "a non-empty top-level mapping with schema-version", "provide schema-version and values"));
        }

        List<ConfigValidationIssue> issues = new ArrayList<ConfigValidationIssue>();
        Object schema = null;
        boolean schemaPresent = false;
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        Set<Object> active = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        active.add(root);
        try {
            for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    issues.add(issue("config.yaml.path", source, "<root>",
                            "string keys with non-blank, non-dotted segments", "rename the YAML key"));
                    continue;
                }
                String key = (String) entry.getKey();
                if ("schema-version".equals(key)) {
                    schema = entry.getValue();
                    schemaPresent = true;
                    continue;
                }
                if (!validSegment(key)) {
                    issues.add(issue("config.yaml.path", source, safePath(key),
                            "non-blank path segments without dots", "rename the YAML key"));
                    continue;
                }
                flatten(source, key, entry.getValue(), values, issues, active, 1);
            }
        } finally {
            active.remove(root);
        }
        if (!schemaPresent || !validSchemaVersion(schema)) {
            issues.add(0, issue("config.yaml.schema-version", source, "schema-version",
                    "a non-negative integral value fitting int",
                    "set schema-version to a supported integer"));
        }
        if (!issues.isEmpty()) {
            return ConfigDocumentLoadResult.failure(ConfigValidationReport.of(issues));
        }
        return ConfigDocumentLoadResult.success(ConfigDocument.of(
                source, ((Number) schema).intValue(), values));
    }

    private static void flatten(String source, String path, Object raw,
                                LinkedHashMap<String, Object> values,
                                List<ConfigValidationIssue> issues,
                                Set<Object> active, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            issues.add(issue("config.yaml.limit", source, safePath(path),
                    "YAML within the configured nesting limit", "reduce the nesting depth"));
            return;
        }
        if (raw instanceof Map && !((Map<?, ?>) raw).isEmpty()) {
            if (!active.add(raw)) {
                issues.add(issue("config.yaml.unsupported-value", source, safePath(path),
                        "a non-recursive supported mapping", "remove the recursive YAML alias"));
                return;
            }
            try {
                for (Map.Entry<?, ?> child : ((Map<?, ?>) raw).entrySet()) {
                    if (!(child.getKey() instanceof String)) {
                        issues.add(issue("config.yaml.path", source, safePath(path),
                                "string keys with non-blank, non-dotted segments", "rename the YAML key"));
                        continue;
                    }
                    String segment = (String) child.getKey();
                    String childPath = safePath(path + "." + segment);
                    if (!validSegment(segment)) {
                        issues.add(issue("config.yaml.path", source, childPath,
                                "non-blank path segments without dots", "rename the YAML key"));
                    } else if ("schema-version".equals(segment)) {
                        issues.add(issue("config.yaml.path", source, childPath,
                                "schema-version only at the document root",
                                "rename the nested metadata key"));
                    } else {
                        flatten(source, path + "." + segment, child.getValue(), values,
                                issues, active, depth + 1);
                    }
                }
            } finally {
                active.remove(raw);
            }
            return;
        }
        Object normalized = normalize(source, path, raw, issues, active, depth);
        if (normalized == INVALID) {
            return;
        }
        if (hasCollision(values, path)) {
            issues.add(issue("config.yaml.path", source, safePath(path),
                    "unique, non-colliding flattened paths", "rename the colliding YAML keys"));
        } else {
            values.put(path, normalized);
        }
    }

    private static Object normalize(String source, String path, Object raw,
                                    List<ConfigValidationIssue> issues,
                                    Set<Object> active, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            issues.add(issue("config.yaml.limit", source, safePath(path),
                    "YAML within the configured nesting limit", "reduce the nesting depth"));
            return INVALID;
        }
        if (raw == null || raw instanceof String || raw instanceof Boolean || raw instanceof Number) {
            return raw;
        }
        if (raw instanceof List) {
            if (!active.add(raw)) {
                issues.add(unsupported(source, path, "remove the recursive YAML alias"));
                return INVALID;
            }
            List<Object> copy = new ArrayList<Object>(((List<?>) raw).size());
            boolean invalid = false;
            try {
                int index = 0;
                for (Object item : (List<?>) raw) {
                    Object normalized = normalize(source, path + "[" + index + "]", item,
                            issues, active, depth + 1);
                    if (normalized == INVALID) {
                        invalid = true;
                    } else {
                        copy.add(normalized);
                    }
                    index++;
                }
            } finally {
                active.remove(raw);
            }
            return invalid ? INVALID : Collections.unmodifiableList(copy);
        }
        if (raw instanceof Map) {
            if (!active.add(raw)) {
                issues.add(unsupported(source, path, "remove the recursive YAML alias"));
                return INVALID;
            }
            LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
            boolean invalid = false;
            try {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                    if (!(entry.getKey() instanceof String)) {
                        issues.add(unsupported(source, path, "replace the non-string map key"));
                        invalid = true;
                        continue;
                    }
                    String key = (String) entry.getKey();
                    String childPath = safePath(path + "." + key);
                    if (!validSegment(key)) {
                        issues.add(issue("config.yaml.path", source, childPath,
                                "non-blank path segments without dots", "rename the map key"));
                        invalid = true;
                        continue;
                    }
                    if ("schema-version".equals(key)) {
                        issues.add(issue("config.yaml.path", source, childPath,
                                "schema-version only at the document root",
                                "rename the nested metadata key"));
                        invalid = true;
                        continue;
                    }
                    Object normalized = normalize(source, childPath, entry.getValue(),
                            issues, active, depth + 1);
                    if (normalized == INVALID) {
                        invalid = true;
                    } else {
                        copy.put(key, normalized);
                    }
                }
            } finally {
                active.remove(raw);
            }
            return invalid ? INVALID : Collections.unmodifiableMap(copy);
        }
        issues.add(unsupported(source, path, "replace the unsupported YAML value"));
        return INVALID;
    }

    private static final Object INVALID = new Object();

    private static boolean hasCollision(Map<String, Object> values, String path) {
        if (values.containsKey(path)) {
            return true;
        }
        for (String existing : values.keySet()) {
            if (existing.startsWith(path + ".") || path.startsWith(existing + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean validSchemaVersion(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger) {
            BigInteger integer = value instanceof BigInteger
                    ? (BigInteger) value : BigInteger.valueOf(((Number) value).longValue());
            return integer.signum() >= 0 && integer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
        }
        return false;
    }

    private static ConfigValidationIssue parserFailure(YAMLException error, String source) {
        String message = error.getMessage();
        if (message != null) {
            String lower = message.toLowerCase(Locale.ROOT);
            if (lower.contains("alias") || lower.contains("nesting")
                    || lower.contains("code point") || lower.contains("codepoint")
                    || lower.contains("recursive")) {
                return issue("config.yaml.limit", source, "<root>",
                        "YAML within the configured safety limits", "reduce aliases, nesting, or input size");
            }
        }
        return issue("config.yaml.syntax", source, "<root>",
                "valid YAML using supported safe types", "repair YAML syntax and try again");
    }

    private static ConfigValidationIssue unsupported(String source, String path, String action) {
        return issue("config.yaml.unsupported-value", source, safePath(path),
                "a supported scalar, list, or map", action);
    }

    private static void requireCallerInputs(String source, String yaml, int version) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(yaml, "yaml");
        if (source.trim().isEmpty()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (isAbsolute(source)) {
            throw new IllegalArgumentException("source must be a logical label, not an absolute host path");
        }
        if (version < 0) {
            throw new IllegalArgumentException("freshSchemaVersion must not be negative");
        }
    }

    private static boolean validSegment(String value) {
        if (value == null || value.trim().isEmpty() || value.indexOf('.') >= 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String safePath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<root>";
        }
        StringBuilder safe = new StringBuilder(Math.min(value.length(), MAX_SAFE_PATH_LENGTH));
        for (int i = 0; i < value.length() && safe.length() < MAX_SAFE_PATH_LENGTH; i++) {
            char character = value.charAt(i);
            if ((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '.' || character == '_' || character == '-'
                    || character == '[' || character == ']') {
                safe.append(character);
            } else {
                safe.append('?');
            }
        }
        return safe.length() == 0 ? "<invalid-path>" : safe.toString();
    }

    private static boolean isAbsolute(String value) {
        return value.startsWith("/") || value.startsWith("\\")
                || (value.length() >= 3 && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':' && (value.charAt(2) == '/' || value.charAt(2) == '\\'));
    }

    private static boolean isBlankOrComments(String text) {
        int lineStart = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n' || text.charAt(i) == '\r') {
                String line = text.substring(lineStart, i).trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    return false;
                }
                lineStart = i + 1;
            }
        }
        return true;
    }

    private static ConfigDocumentLoadResult failure(ConfigValidationIssue issue) {
        return ConfigDocumentLoadResult.failure(ConfigValidationReport.of(
                Collections.singletonList(issue)));
    }

    private static ConfigValidationIssue issue(String id, String source, String path,
                                               String expected, String action) {
        return new ConfigValidationIssue(id, ValidationSeverity.ERROR, source, safePath(path),
                "<not-read>", expected, IMPACT, action);
    }
}
