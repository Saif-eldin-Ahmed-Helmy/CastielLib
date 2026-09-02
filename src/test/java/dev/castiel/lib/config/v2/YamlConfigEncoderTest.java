package dev.castiel.lib.config.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlConfigEncoderTest {
    private final YamlConfigEncoder encoder = new YamlConfigEncoder();
    private final YamlConfigDecoder decoder = new YamlConfigDecoder();

    @Test
    void roundTripPreservesOrderNullUnknownAndEmptyContainers() {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("optional", null);
        values.put("known.enabled", Boolean.TRUE);
        values.put("unknown", "value");
        values.put("empty-map", Collections.emptyMap());
        values.put("empty-list", Collections.emptyList());
        ConfigDocument original = ConfigDocument.of("memory", 2, values);

        YamlConfigEncodeResult encoded = encoder.encode(original);
        assertTrue(encoded.isSuccessful(), encoded.toString());
        assertTrue(encoded.yaml().startsWith("schema-version: 2\n"));

        ConfigDocumentLoadResult decoded = decoder.decode("memory", encoded.yaml(), 9);
        assertTrue(decoded.isSuccessful(), decoded.toString());
        assertEquals(new ArrayList<String>(original.values().keySet()),
                new ArrayList<String>(decoded.document().values().keySet()));
        assertTrue(decoded.document().contains("optional"));
        assertEquals(null, decoded.document().value("optional"));
        assertEquals(Collections.emptyMap(), decoded.document().value("empty-map"));
        assertEquals(Collections.emptyList(), decoded.document().value("empty-list"));
    }

    @Test
    void outputIsDeterministicAndDoesNotContainAliasesOrRuntimeTags() {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        List<String> shared = new ArrayList<String>(Collections.singletonList("one"));
        values.put("first", shared);
        values.put("second", shared);
        ConfigDocument document = ConfigDocument.of("memory", 1, values);

        String first = encoder.encode(document).yaml();
        String second = encoder.encode(document).yaml();
        assertEquals(first, second);
        assertFalse(first.contains("&id"));
        assertFalse(first.contains("*id"));
        assertFalse(first.contains("!!java"));
    }

    @Test
    void collisionsAndReservedPathsFailClosedWithoutYaml() {
        LinkedHashMap<String, Object> collision = new LinkedHashMap<String, Object>();
        collision.put("a", "one");
        collision.put("a.b", "two");
        YamlConfigEncodeResult collisionResult = encoder.encode(ConfigDocument.of("memory", 1, collision));
        assertFalse(collisionResult.isSuccessful());
        assertThrows(IllegalStateException.class, collisionResult::yaml);
        assertEquals("config.yaml.path", collisionResult.report().issues().get(0).id());

        for (String path : Arrays.asList("schema-version", "a.schema-version", "a..b", ".a", "a.")) {
            LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
            values.put(path, "value");
            YamlConfigEncodeResult result = encoder.encode(ConfigDocument.of("memory", 1, values));
            assertFalse(result.isSuccessful(), path);
            assertThrows(IllegalStateException.class, result::yaml);
        }
    }

    @Test
    void supportedScalarsAndNestedCollectionsEncode() {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("byte", Byte.valueOf((byte) 1));
        values.put("short", Short.valueOf((short) 2));
        values.put("integer", Integer.valueOf(3));
        values.put("long", Long.valueOf(4L));
        values.put("float", Float.valueOf(1.5F));
        values.put("double", Double.valueOf(2.5D));
        values.put("big-integer", new BigInteger("12345678901234567890"));
        values.put("big-decimal", new BigDecimal("3.14159"));
        values.put("nested", Arrays.asList(true, null,
                Collections.singletonMap("child", "value")));
        YamlConfigEncodeResult result = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertTrue(result.isSuccessful(), result.toString());
        assertTrue(decoder.decode("memory", result.yaml(), 1).isSuccessful());
    }

    @Test
    void unsupportedValuesRecursiveGraphsAndUnsafeKeysFailWithoutStringification() {
        ThrowingNumber number = new ThrowingNumber();
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("number", number);
        YamlConfigEncodeResult unsupported = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertFalse(unsupported.isSuccessful());
        assertEquals("config.yaml.unsupported-value", unsupported.report().issues().get(0).id());

        List<Object> recursive = new ArrayList<Object>();
        recursive.add(recursive);
        values.clear();
        values.put("loop", recursive);
        YamlConfigEncodeResult recursiveResult = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertFalse(recursiveResult.isSuccessful());

        Map<Object, Object> unsafe = new LinkedHashMap<Object, Object>();
        unsafe.put(new ThrowingKey(), "value");
        values.clear();
        values.put("map", unsafe);
        YamlConfigEncodeResult unsafeResult = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertFalse(unsafeResult.isSuccessful());
        assertFalse(unsafeResult.toString().contains("secret-key"));
        assertFalse(unsafeResult.report().issues().get(0).suppliedValue().contains("secret-key"));
    }

    @Test
    void nestedReservedAndControlCharacterKeysFailWithSafePaths() {
        LinkedHashMap<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("schema-version", 2);
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("section", nested);
        YamlConfigEncodeResult reserved = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertFalse(reserved.isSuccessful());
        assertEquals("config.yaml.path", reserved.report().issues().get(0).id());

        nested.clear();
        nested.put("bad\nkey", "value");
        YamlConfigEncodeResult control = encoder.encode(ConfigDocument.of("memory", 1, values));
        assertFalse(control.isSuccessful());
        for (ConfigValidationIssue issue : control.report().issues()) {
            assertFalse(issue.path().contains("\n"));
            assertFalse(issue.path().contains("\r"));
        }
    }

    private static final class ThrowingNumber extends Number {
        @Override public int intValue() { return 1; }
        @Override public long longValue() { return 1L; }
        @Override public float floatValue() { return 1.0F; }
        @Override public double doubleValue() { return 1.0D; }
        @Override public String toString() { throw new AssertionError("secret-number"); }
    }

    private static final class ThrowingKey {
        @Override public String toString() { throw new AssertionError("secret-key"); }
    }
}
