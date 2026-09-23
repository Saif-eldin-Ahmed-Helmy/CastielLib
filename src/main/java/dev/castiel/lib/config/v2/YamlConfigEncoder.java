package dev.castiel.lib.config.v2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Deterministic, fail-closed YAML encoder for the format-neutral config document. */
public final class YamlConfigEncoder {
    private static final String IMPACT = "configuration cannot be saved";
    private static final int MAX_NESTING_DEPTH = 64;
    private static final int MAX_SAFE_PATH_LENGTH = 128;

    public YamlConfigEncoder() { }

    /**
     * Encodes one immutable document without retaining caller-owned values.
     *
     * @param document document to encode
     * @return deterministic YAML or safe actionable failures
     */
    public YamlConfigEncodeResult encode(ConfigDocument document) {
        ConfigDocument checked = Objects.requireNonNull(document, "document");
        List<ConfigValidationIssue> issues = new ArrayList<ConfigValidationIssue>();
        Node root = new Node();
        Node schema = new Node();
        schema.value = Integer.valueOf(checked.schemaVersion());
        schema.hasValue = true;
        root.children.put("schema-version", schema);

        for (Map.Entry<String, Object> entry : checked.values().entrySet()) {
            insert(checked.source(), root, entry.getKey(), entry.getValue(), issues);
        }
        if (!issues.isEmpty()) {
            return YamlConfigEncodeResult.failure(ConfigValidationReport.of(issues));
        }

        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(false);
            options.setCanonical(false);
            options.setExplicitStart(false);
            options.setExplicitEnd(false);
            options.setSplitLines(false);
            options.setIndent(2);
            options.setWidth(4096);
            options.setAllowUnicode(true);
            return YamlConfigEncodeResult.success(new Yaml(options).dump(root.toValue()));
        } catch (RuntimeException ignored) {
            return YamlConfigEncodeResult.failure(ConfigValidationReport.of(
                    Collections.singletonList(issue("config.yaml.unsupported-value", checked.source(),
                            "<root>", "safe YAML-supported values", "remove the value that cannot be encoded"))));
        }
    }

    private static void insert(String source, Node root, String path, Object raw,
                               List<ConfigValidationIssue> issues) {
        String[] segments = pathSegments(source, path, issues);
        if (segments == null) {
            return;
        }
        Node current = root;
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            Node next = current.children.get(segment);
            if (next == null) {
                next = new Node();
                current.children.put(segment, next);
            }
            boolean last = index == segments.length - 1;
            if (!last) {
                if (next.hasValue) {
                    issues.add(issue("config.yaml.path", source, safePath(path),
                            "unique, non-colliding flattened paths", "rename the colliding YAML paths"));
                    return;
                }
                current = next;
            } else {
                if (next.hasValue || !next.children.isEmpty()) {
                    issues.add(issue("config.yaml.path", source, safePath(path),
                            "unique, non-colliding flattened paths", "rename the colliding YAML paths"));
                    return;
                }
                Object normalized = normalize(source, path, raw, issues,
                        Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()), 1);
                if (normalized != INVALID) {
                    next.value = normalized;
                    next.hasValue = true;
                }
            }
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
        if (raw == null || raw instanceof String || raw instanceof Boolean
                || raw instanceof Byte || raw instanceof Short || raw instanceof Integer
                || raw instanceof Long || raw instanceof Float || raw instanceof Double
                || raw instanceof BigInteger || raw instanceof BigDecimal) {
            return raw;
        }
        if (raw instanceof List) {
            if (!active.add(raw)) {
                issues.add(unsupported(source, path, "remove the recursive value"));
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
                issues.add(unsupported(source, path, "remove the recursive value"));
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
                    String childPath = path + "." + key;
                    if (!validSegment(key)) {
                        issues.add(issue("config.yaml.path", source, safePath(childPath),
                                "non-blank path segments without dots or control characters",
                                "rename the map key"));
                        invalid = true;
                        continue;
                    }
                    if ("schema-version".equals(key)) {
                        issues.add(issue("config.yaml.path", source, safePath(childPath),
                                "schema-version only at the document root",
                                "rename the nested metadata key"));
                        invalid = true;
                        continue;
                    }
                    if (copy.containsKey(key)) {
                        issues.add(issue("config.yaml.path", source, safePath(childPath),
                                "unique map keys", "remove the duplicate map key"));
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
        issues.add(unsupported(source, path, "replace the unsupported value"));
        return INVALID;
    }

    private static String[] pathSegments(String source, String path,
                                         List<ConfigValidationIssue> issues) {
        if (path == null || path.trim().isEmpty()) {
            issues.add(issue("config.yaml.path", source, "<root>",
                    "non-blank path segments", "provide a configuration path"));
            return null;
        }
        String[] segments = path.split("\\.", -1);
        for (String segment : segments) {
            if (!validSegment(segment) || "schema-version".equals(segment)) {
                issues.add(issue("config.yaml.path", source, safePath(path),
                        "non-blank non-reserved path segments without dots or control characters",
                        "rename the configuration path"));
                return null;
            }
        }
        return segments;
    }

    private static boolean validSegment(String value) {
        if (value == null || value.trim().isEmpty() || value.indexOf('.') >= 0) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
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
        for (int index = 0; index < value.length() && safe.length() < MAX_SAFE_PATH_LENGTH; index++) {
            char character = value.charAt(index);
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

    private static ConfigValidationIssue unsupported(String source, String path, String action) {
        return issue("config.yaml.unsupported-value", source, safePath(path),
                "a supported scalar, list, or map", action);
    }

    private static ConfigValidationIssue issue(String id, String source, String path,
                                               String expected, String action) {
        return new ConfigValidationIssue(id, ValidationSeverity.ERROR, source, safePath(path),
                "<not-read>", expected, IMPACT, action);
    }

    private static final Object INVALID = new Object();

    private static final class Node {
        private final LinkedHashMap<String, Node> children = new LinkedHashMap<String, Node>();
        private boolean hasValue;
        private Object value;

        private Object toValue() {
            if (hasValue) {
                return value;
            }
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Node> entry : children.entrySet()) {
                map.put(entry.getKey(), entry.getValue().toValue());
            }
            return map;
        }
    }
}
