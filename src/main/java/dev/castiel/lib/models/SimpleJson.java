package dev.castiel.lib.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private final String text;
    private int index;

    private SimpleJson(String text) {
        this.text = text == null ? "" : text;
    }

    static Map<String, Object> parseObject(String text) {
        Object value = new SimpleJson(text).readValue();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected JSON object.");
        }
        return (Map<String, Object>) value;
    }

    private Object readValue() {
        skipWhitespace();
        char c = peek();
        if (c == '{') {
            return readObject();
        }
        if (c == '[') {
            return readArray();
        }
        if (c == '"') {
            return readString();
        }
        if (c == 't' || c == 'f') {
            return readBoolean();
        }
        if (c == 'n') {
            expect("null");
            return null;
        }
        return readNumber();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        expect('{');
        skipWhitespace();
        if (consume('}')) {
            return result;
        }
        do {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            result.put(key, readValue());
            skipWhitespace();
        } while (consume(','));
        expect('}');
        return result;
    }

    private List<Object> readArray() {
        List<Object> result = new ArrayList<Object>();
        expect('[');
        skipWhitespace();
        if (consume(']')) {
            return result;
        }
        do {
            result.add(readValue());
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return result;
    }

    private String readString() {
        StringBuilder builder = new StringBuilder();
        expect('"');
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') {
                return builder.toString();
            }
            builder.append(c == '\\' ? readEscaped() : c);
        }
        throw error("Unterminated string.");
    }

    private char readEscaped() {
        char c = next();
        if (c == 'u') {
            return (char) Integer.parseInt(text.substring(index, index += 4), 16);
        }
        if (c == 'n') return '\n';
        if (c == 'r') return '\r';
        if (c == 't') return '\t';
        if (c == 'b') return '\b';
        if (c == 'f') return '\f';
        return c;
    }

    private Boolean readBoolean() {
        if (text.startsWith("true", index)) {
            index += 4;
            return Boolean.TRUE;
        }
        expect("false");
        return Boolean.FALSE;
    }

    private Number readNumber() {
        int start = index;
        while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0) {
            index++;
        }
        String raw = text.substring(start, index);
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
            return Double.parseDouble(raw);
        }
        return Long.parseLong(raw);
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private boolean consume(char expected) {
        if (peek() != expected) {
            return false;
        }
        index++;
        return true;
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("Expected '" + expected + "'.");
        }
    }

    private void expect(String expected) {
        if (!text.startsWith(expected, index)) {
            throw error("Expected " + expected + ".");
        }
        index += expected.length();
    }

    private char peek() {
        return index < text.length() ? text.charAt(index) : '\0';
    }

    private char next() {
        if (index >= text.length()) {
            throw error("Unexpected end of JSON.");
        }
        return text.charAt(index++);
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " index=" + index);
    }
}
