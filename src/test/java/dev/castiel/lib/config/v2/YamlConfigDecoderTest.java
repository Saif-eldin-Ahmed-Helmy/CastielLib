package dev.castiel.lib.config.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class YamlConfigDecoderTest {
    private final YamlConfigDecoder decoder = new YamlConfigDecoder();

    @Test
    void callerValidationAndBlankDocumentsAreExact() {
        assertThrows(NullPointerException.class, () -> decoder.decode(null, "", 1));
        assertThrows(NullPointerException.class, () -> decoder.decode("source", null, 1));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(" ", "", 1));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("/etc/config.yml", "", 1));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("C:\\config.yml", "", 1));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("source", "", -1));

        for (String yaml : new String[] {"", "  \n\t", "# comment\n# another\r\n"}) {
            ConfigDocumentLoadResult result = decoder.decode("defaults", yaml, 4);
            assertTrue(result.isSuccessful());
            assertEquals(ConfigDocument.fresh("defaults", 4).toString(), result.document().toString());
            assertTrue(result.document().values().isEmpty());
            assertTrue(result.report().isValid());
        }
    }

    @Test
    void explicitNullIsPresentAndDistinguishedFromAbsence() {
        ConfigDocument document = success("schema-version: 2\noptional: null\nnested:\n  value: null\n");
        assertTrue(document.contains("optional"));
        assertTrue(document.contains("nested.value"));
        assertEquals(null, document.value("optional"));
        assertFalse(document.contains("missing"));
        assertEquals(Arrays.asList("optional", "nested.value"),
                new ArrayList<String>(document.values().keySet()));
    }

    @Test
    void validValuesFlattenInOrderAndCollectionsAreDeeplyImmutable() {
        ConfigDocument document = success("schema-version: 2\n"
                + "known:\n  enabled: true\n  count: 2\n  ratio: 2.5\n  text: hello\n"
                + "unknown: [one, null, {nested: value, empty: {}}]\n"
                + "empty-map: {}\nempty-list: []\n");
        assertEquals(Arrays.asList("known.enabled", "known.count", "known.ratio", "known.text",
                "unknown", "empty-map", "empty-list"),
                new ArrayList<String>(document.values().keySet()));
        assertEquals(Boolean.TRUE, document.value("known.enabled"));
        assertEquals(Integer.valueOf(2), document.value("known.count"));
        assertEquals(Double.valueOf(2.5D), document.value("known.ratio"));
        assertEquals(Collections.emptyMap(), document.value("empty-map"));
        assertEquals(Collections.emptyList(), document.value("empty-list"));
        List<?> unknown = (List<?>) document.value("unknown");
        assertThrows(UnsupportedOperationException.class, () -> unknown.clear());
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map<?, ?>) unknown.get(2)).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map) unknown.get(2)).put("other", "value"));
    }

    @Test
    void schemaVersionMatrixFailsClosedAndAcceptsIntegerBoundaries() {
        assertEquals(0, decoder.decode("memory", "schema-version: 0\n", 9)
                .document().schemaVersion());
        assertEquals(Integer.MAX_VALUE, decoder.decode("memory",
                "schema-version: 2147483647\n", 9).document().schemaVersion());
        for (String yaml : new String[] {
                "value: x\n", "schema-version:\n", "schema-version: one\n",
                "schema-version: true\n", "schema-version: 1.5\n",
                "schema-version: -1\n", "schema-version: 2147483648\n",
                "schema-version: [1]\n", "schema-version: {nested: 1}\n"}) {
            ConfigDocumentLoadResult result = decoder.decode("memory", yaml, 1);
            assertFalse(result.isSuccessful(), yaml);
            assertEquals("config.yaml.schema-version", result.report().issues().get(0).id());
            assertThrows(IllegalStateException.class, result::document);
        }
    }

    @Test
    void rootShapeKeysDocumentsAndFlattenedCollisionsFailClosed() {
        for (String yaml : new String[] {"hello", "[one, two]\n", "{}\n",
                "schema-version: 1\n---\nschema-version: 1\n", 
                "schema-version: 1\n'a.b': 1\n", "schema-version: 1\n' ': 1\n",
                "schema-version: 1\na: 1\na:\n  child: 2\n",
                "schema-version: 1\na:\n  child: 1\n'a.child': 2\n"}) {
            ConfigDocumentLoadResult result = decoder.decode("memory", yaml, 1);
            assertFalse(result.isSuccessful(), yaml);
            assertThrows(IllegalStateException.class, result::document);
        }
        ConfigDocumentLoadResult nonStringKey = decoder.decode("memory",
                "schema-version: 1\ntrue: value\n", 1);
        assertFalse(nonStringKey.isSuccessful());
        assertTrue(nonStringKey.report().issues().stream()
                .anyMatch(issue -> "config.yaml.path".equals(issue.id())));
    }

    @Test
    void nestedReservedMetadataAndDuplicateKeysFailWithSafeLocations() {
        ConfigDocumentLoadResult nestedReserved = decoder.decode("memory",
                "schema-version: 1\nsection:\n  schema-version: 2\n", 1);
        assertFalse(nestedReserved.isSuccessful());
        assertTrue(nestedReserved.report().issues().stream()
                .anyMatch(issue -> "config.yaml.path".equals(issue.id())));
        assertThrows(IllegalStateException.class, nestedReserved::document);

        ConfigDocumentLoadResult duplicate = decoder.decode("memory",
                "schema-version: 1\nvalue: one\nvalue: two\n", 1);
        assertFalse(duplicate.isSuccessful());
        assertEquals("config.yaml.syntax", duplicate.report().issues().get(0).id());

        ConfigDocumentLoadResult hostileKey = decoder.decode("memory",
                "schema-version: 1\n\"bad\\nkey\": value\n", 1);
        assertFalse(hostileKey.isSuccessful());
        assertFalse(hostileKey.toString().contains("bad\\nkey"));
        for (ConfigValidationIssue issue : hostileKey.report().issues()) {
            assertFalse(issue.path().contains("\n"));
            assertFalse(issue.path().contains("\r"));
        }
    }

    @Test
    void unsupportedTypesAliasesAndCustomTagsFailWithoutUnsafeValues() {
        for (String yaml : new String[] {
                "schema-version: 1\ndate: 2020-01-01\n",
                "schema-version: 1\nbinary: !!binary SGVsbG8=\n",
                "schema-version: 1\nset: !!set {one: null}\n",
                "schema-version: 1\ncustom: !application secret\n",
                "schema-version: 1\nloop: &loop {self: *loop}\n"}) {
            ConfigDocumentLoadResult result = decoder.decode("memory", yaml, 1);
            assertFalse(result.isSuccessful(), yaml);
            assertThrows(IllegalStateException.class, result::document);
            assertFalse(result.toString().contains("secret"));
            assertTrue(result.report().issues().get(0).suppliedValue().equals("<not-read>"));
        }
    }

    @Test
    void aliasesNestingAndInputSizeAreBounded() {
        StringBuilder aliases = new StringBuilder("schema-version: 1\nbase: &base {value: 1}\n");
        for (int i = 0; i < 40; i++) {
            aliases.append("alias").append(i).append(": *base\n");
        }
        assertLimit(decoder.decode("memory", aliases.toString(), 1));

        StringBuilder deep = new StringBuilder("schema-version: 1\n");
        for (int i = 0; i < 70; i++) {
            deep.append("level").append(i).append(":\n");
            for (int j = 0; j <= i; j++) {
                deep.append("  ");
            }
        }
        deep.append("value: 1\n");
        assertLimit(decoder.decode("memory", deep.toString(), 1));

        StringBuilder huge = new StringBuilder(1_000_001);
        while (huge.length() < 1_000_001) {
            huge.append('#');
        }
        assertLimit(decoder.decode("memory", huge.toString(), 1));
    }

    @Test
    void postParseErrorsAggregateInStableSafeOrder() {
        ConfigDocumentLoadResult result = decoder.decode("memory", "schema-version: nope\n"
                + "'bad.key': 1\nfeatures:\n  ' ': 2\n"
                + "timestamp: 2020-01-01\n", 1);
        assertFalse(result.isSuccessful());
        assertEquals(Arrays.asList("config.yaml.schema-version", "config.yaml.path",
                "config.yaml.path", "config.yaml.unsupported-value"),
                issueIds(result));
        assertFalse(result.toString().contains("nope"));
        assertFalse(result.report().toString().contains("2020-01-01"));
    }

    @Test
    void p009MigrationAndP007ResolutionRetainNullUnknownOrder() {
        ConfigDocument decoded = success("schema-version: 0\noptional: null\nunknown: value\n");
        ConfigMigration step = new ConfigMigration() {
            @Override public int fromVersion() { return 0; }
            @Override public Set<String> changedPaths() { return Collections.emptySet(); }
            @Override public ConfigDocument migrate(ConfigDocument document) {
                return document.advanceTo(1);
            }
        };
        ConfigMigrationResult migrated = new ConfigMigrator(1,
                Collections.singletonList(step)).migrate(decoded);
        assertTrue(migrated.isSuccessful());
        assertEquals(Arrays.asList("optional", "unknown"),
                new ArrayList<String>(migrated.document().values().keySet()));
        assertTrue(migrated.document().contains("optional"));
        assertEquals(null, migrated.document().value("optional"));

        ConfigKey<String> known = new ConfigKey<String>("known", String.class, "default",
                value -> true, "text", "feature", "set text", false);
        ConfigSnapshot snapshot = ConfigSchema.of(Collections.<ConfigKey<?>>singletonList(known))
                .resolve(migrated.document().source(), migrated.document().values());
        assertTrue(snapshot.isValid());
        assertEquals("default", snapshot.get(known));
        assertEquals(Arrays.asList("optional", "unknown"),
                Arrays.asList(snapshot.report().warnings().get(0).path(),
                        snapshot.report().warnings().get(1).path()));
    }

    private ConfigDocument success(String yaml) {
        ConfigDocumentLoadResult result = decoder.decode("memory", yaml, 9);
        assertTrue(result.isSuccessful(), result.toString());
        return result.document();
    }

    private static List<String> issueIds(ConfigDocumentLoadResult result) {
        List<String> ids = new ArrayList<String>();
        for (ConfigValidationIssue issue : result.report().issues()) {
            ids.add(issue.id());
        }
        return ids;
    }

    private static void assertLimit(ConfigDocumentLoadResult result) {
        assertFalse(result.isSuccessful());
        assertEquals("config.yaml.limit", result.report().issues().get(0).id());
    }
}
