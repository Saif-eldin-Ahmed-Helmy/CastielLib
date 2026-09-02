package dev.castiel.lib.config.v2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stateless, bounded planner for safe configuration previews. */
public final class ConfigChangePlanner {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_COLLECTION_ENTRIES = 10000;
    private static final int MAX_SAFE_PATH_LENGTH = 128;

    public ConfigChangePlanner() { }

    /**
     * Computes a value-free deterministic plan without retaining either document.
     *
     * @param before prior document
     * @param after candidate document
     * @return immutable preview plan
     */
    public ConfigChangePlan plan(ConfigDocument before, ConfigDocument after) {
        ConfigDocument previous = Objects.requireNonNull(before, "before");
        ConfigDocument next = Objects.requireNonNull(after, "after");
        List<String> paths = new ArrayList<String>();
        paths.add("schema-version");
        for (String path : previous.values().keySet()) {
            if (!"schema-version".equals(path)) {
                paths.add(path);
            }
        }
        for (String path : next.values().keySet()) {
            if (!"schema-version".equals(path) && !previous.values().containsKey(path)) {
                paths.add(path);
            }
        }
        List<ConfigChange> changes = new ArrayList<ConfigChange>();
        for (String path : paths) {
            boolean schemaPath = "schema-version".equals(path);
            boolean beforePresent = schemaPath || previous.values().containsKey(path);
            boolean afterPresent = schemaPath || next.values().containsKey(path);
            String safePath = safePath(path);
            if (!beforePresent) {
                changes.add(new ConfigChange(safePath, ConfigChange.Kind.ADDED,
                        "<absent>", descriptor(next.values().get(path))));
                continue;
            }
            if (!afterPresent) {
                changes.add(new ConfigChange(safePath, ConfigChange.Kind.REMOVED,
                        descriptor(previous.values().get(path)), "<absent>"));
                continue;
            }
            Object oldValue = schemaPath ? Integer.valueOf(previous.schemaVersion())
                    : previous.values().get(path);
            Object newValue = schemaPath ? Integer.valueOf(next.schemaVersion())
                    : next.values().get(path);
            if (!equivalent(oldValue, newValue, 0,
                    new IdentityHashMap<Object, IdentityHashMap<Object, Boolean>>())) {
                changes.add(new ConfigChange(safePath, ConfigChange.Kind.CHANGED,
                        descriptor(oldValue), descriptor(newValue)));
            } else if (indexOf(previous, path) != indexOf(next, path)) {
                changes.add(new ConfigChange(safePath, ConfigChange.Kind.ORDER_CHANGED,
                        descriptor(oldValue), descriptor(newValue)));
            }
        }
        return ConfigChangePlan.of(changes);
    }

    private static int indexOf(ConfigDocument document, String target) {
        if ("schema-version".equals(target)) {
            return 0;
        }
        int index = 1;
        for (String path : document.values().keySet()) {
            if ("schema-version".equals(path)) {
                continue;
            }
            if (target.equals(path)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static boolean equivalent(Object before, Object after, int depth,
                                      IdentityHashMap<Object, IdentityHashMap<Object, Boolean>> active) {
        if (before == null || after == null) {
            return before == after;
        }
        if (isScalar(before) || isScalar(after)) {
            return isScalar(before) && isScalar(after)
                    && before.getClass() == after.getClass() && before.equals(after);
        }
        if (depth >= MAX_DEPTH || !trustedCollection(before) || !trustedCollection(after)) {
            return false;
        }
        if (!enterPair(active, before, after)) {
            return false;
        }
        try {
            if (before instanceof List && after instanceof List) {
                List<?> left = (List<?>) before;
                List<?> right = (List<?>) after;
                if (left.size() != right.size() || left.size() > MAX_COLLECTION_ENTRIES) {
                    return false;
                }
                Iterator<?> leftIterator = left.iterator();
                Iterator<?> rightIterator = right.iterator();
                int count = 0;
                while (leftIterator.hasNext() && rightIterator.hasNext()) {
                    if (++count > MAX_COLLECTION_ENTRIES) {
                        return false;
                    }
                    if (!equivalent(leftIterator.next(), rightIterator.next(), depth + 1, active)) {
                        return false;
                    }
                }
                return !leftIterator.hasNext() && !rightIterator.hasNext();
            }
            if (before instanceof Map && after instanceof Map) {
                Map<?, ?> left = (Map<?, ?>) before;
                Map<?, ?> right = (Map<?, ?>) after;
                if (left.size() != right.size() || left.size() > MAX_COLLECTION_ENTRIES) {
                    return false;
                }
                Iterator<? extends Map.Entry<?, ?>> leftIterator = left.entrySet().iterator();
                Iterator<? extends Map.Entry<?, ?>> rightIterator = right.entrySet().iterator();
                int count = 0;
                while (leftIterator.hasNext() && rightIterator.hasNext()) {
                    if (++count > MAX_COLLECTION_ENTRIES) {
                        return false;
                    }
                    Map.Entry<?, ?> leftEntry = leftIterator.next();
                    Map.Entry<?, ?> rightEntry = rightIterator.next();
                    if (!(leftEntry.getKey() instanceof String)
                            || !(rightEntry.getKey() instanceof String)
                            || !leftEntry.getKey().equals(rightEntry.getKey())
                            || !equivalent(leftEntry.getValue(), rightEntry.getValue(), depth + 1, active)) {
                        return false;
                    }
                }
                return !leftIterator.hasNext() && !rightIterator.hasNext();
            }
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            leavePair(active, before, after);
        }
    }

    private static boolean enterPair(IdentityHashMap<Object, IdentityHashMap<Object, Boolean>> active,
                                     Object before, Object after) {
        IdentityHashMap<Object, Boolean> afterValues = active.get(before);
        if (afterValues == null) {
            afterValues = new IdentityHashMap<Object, Boolean>();
            active.put(before, afterValues);
        }
        if (afterValues.containsKey(after)) {
            return false;
        }
        afterValues.put(after, Boolean.TRUE);
        return true;
    }

    private static void leavePair(IdentityHashMap<Object, IdentityHashMap<Object, Boolean>> active,
                                  Object before, Object after) {
        IdentityHashMap<Object, Boolean> afterValues = active.get(before);
        if (afterValues != null) {
            afterValues.remove(after);
            if (afterValues.isEmpty()) {
                active.remove(before);
            }
        }
    }

    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Boolean || isStandardNumber(value);
    }

    private static boolean isStandardNumber(Object value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Float || value instanceof Double
                || value instanceof BigInteger || value instanceof BigDecimal;
    }

    private static boolean trustedCollection(Object value) {
        if (!(value instanceof List) && !(value instanceof Map)) {
            return false;
        }
        return value.getClass().getName().startsWith("java.util.");
    }

    private static String descriptor(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value instanceof String) {
            return "<string>";
        }
        if (value instanceof Boolean) {
            return "<boolean>";
        }
        if (isStandardNumber(value)) {
            return "<number>";
        }
        if (value instanceof List && trustedCollection(value)
                && safeCollection(value, 0, new IdentityHashMap<Object, Boolean>(), new int[1])) {
            return "<list>";
        }
        if (value instanceof Map && trustedCollection(value)
                && safeCollection(value, 0, new IdentityHashMap<Object, Boolean>(), new int[1])) {
            return "<map>";
        }
        return "<unsupported>";
    }

    private static boolean safeCollection(Object value, int depth,
                                          IdentityHashMap<Object, Boolean> active, int[] count) {
        if (++count[0] > MAX_COLLECTION_ENTRIES) {
            return false;
        }
        if (value == null || isScalar(value)) {
            return true;
        }
        if (depth >= MAX_DEPTH || !trustedCollection(value)
                || active.put(value, Boolean.TRUE) != null) {
            return false;
        }
        try {
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (!safeCollection(item, depth + 1, active, count)) {
                        return false;
                    }
                }
                return true;
            }
            if (value instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (!(entry.getKey() instanceof String)
                            || !safeCollection(entry.getValue(), depth + 1, active, count)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            active.remove(value);
        }
    }

    private static String safePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "<root>";
        }
        StringBuilder safe = new StringBuilder(Math.min(path.length(), MAX_SAFE_PATH_LENGTH));
        for (int index = 0; index < path.length() && safe.length() < MAX_SAFE_PATH_LENGTH; index++) {
            char character = path.charAt(index);
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
}
