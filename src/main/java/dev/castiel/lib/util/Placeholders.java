package dev.castiel.lib.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Placeholders {
    private final Map<String, String> values = new LinkedHashMap<>();

    public static Placeholders empty() {
        return new Placeholders();
    }

    public static Placeholders of(String key, Object value) {
        return new Placeholders().put(key, value);
    }

    public Placeholders put(String key, Object value) {
        values.put(key, String.valueOf(value));
        return this;
    }

    public String apply(String input) {
        String out = input == null ? "" : input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            out = out.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return out;
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
