package dev.castiel.lib.config.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic runner for a complete contiguous set of one-step migrations.
 *
 * <p>The plan is validated and copied at construction. Execution is strictly
 * forward and preserves every value outside each step's declared paths by
 * object identity and relative encounter order.</p>
 */
public final class ConfigMigrator {
    private static final Pattern FLAT_PATH =
            Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");
    private static final String IMPACT = "configuration cannot be loaded";

    private final int targetVersion;
    private final List<Step> steps;

    /**
     * Creates a migrator with exactly one step for every version before the
     * target.
     *
     * @param targetVersion final supported schema version
     * @param migrations one-step migrations, in any order
     * @throws NullPointerException if the list, an entry, or a changed-path
     *     set is null
     * @throws IllegalArgumentException if the target or plan is invalid
     */
    public ConfigMigrator(int targetVersion, List<ConfigMigration> migrations) {
        if (targetVersion < 0) {
            throw new IllegalArgumentException("targetVersion must not be negative");
        }
        this.targetVersion = targetVersion;
        List<ConfigMigration> copied = new ArrayList<ConfigMigration>(
                Objects.requireNonNull(migrations, "migrations"));
        Map<Integer, Step> byVersion = new HashMap<Integer, Step>();
        for (ConfigMigration migration : copied) {
            ConfigMigration checked = Objects.requireNonNull(migration, "migration");
            int fromVersion = checked.fromVersion();
            if (fromVersion < 0 || fromVersion >= targetVersion) {
                throw new IllegalArgumentException("migration fromVersion is outside the target range");
            }
            if (byVersion.containsKey(Integer.valueOf(fromVersion))) {
                throw new IllegalArgumentException("duplicate migration fromVersion: " + fromVersion);
            }
            byVersion.put(Integer.valueOf(fromVersion),
                    new Step(snapshotPaths(checked.changedPaths()), checked));
        }
        List<Step> ordered = new ArrayList<Step>(targetVersion);
        for (int version = 0; version < targetVersion; version++) {
            Step step = byVersion.get(Integer.valueOf(version));
            if (step == null) {
                throw new IllegalArgumentException("missing migration fromVersion: " + version);
            }
            ordered.add(step);
        }
        this.steps = Collections.unmodifiableList(ordered);
    }

    /** @return final schema version accepted by this migrator */
    public int targetVersion() {
        return targetVersion;
    }

    /**
     * Runs the contiguous suffix needed by one document.
     *
     * @param document immutable source document
     * @return successful final document or one actionable failure report
     * @throws NullPointerException if document is null
     */
    public ConfigMigrationResult migrate(ConfigDocument document) {
        ConfigDocument current = Objects.requireNonNull(document, "document");
        int inputVersion = current.schemaVersion();
        if (inputVersion > targetVersion) {
            return failure(current, "config.version.future", inputVersion,
                    "schema version " + targetVersion + " or older",
                    "downgrade is not supported; update the migrator or restore a compatible configuration");
        }
        if (inputVersion == targetVersion) {
            return ConfigMigrationResult.success(current);
        }

        for (int version = inputVersion; version < targetVersion; version++) {
            Step step = steps.get(version);
            ConfigDocument output;
            try {
                output = step.migration.migrate(current);
            } catch (RuntimeException ignored) {
                return failure(current, "config.migration.failed", version,
                        "step " + version + " -> " + (version + 1),
                        "repair the migration for this step and retry");
            }
            if (output == null || !hasValidOutput(current, output, step.changedPaths)) {
                return failure(current, "config.migration.invalid-output", version,
                        "step " + version + " -> " + (version + 1)
                                + " must preserve source and advance exactly one version",
                        "repair the migration output for this step and retry");
            }
            current = output;
        }
        return ConfigMigrationResult.success(current);
    }

    private static Set<String> snapshotPaths(Set<String> paths) {
        Set<String> copy = new LinkedHashSet<String>();
        for (String path : Objects.requireNonNull(paths, "changedPaths")) {
            if (path == null) {
                throw new NullPointerException("changedPaths element");
            }
            if (path.trim().isEmpty() || !FLAT_PATH.matcher(path).matches()) {
                throw new IllegalArgumentException("changed path must be a non-blank flat path");
            }
            copy.add(path);
        }
        return Collections.unmodifiableSet(copy);
    }

    private static boolean hasValidOutput(ConfigDocument before, ConfigDocument after,
                                          Set<String> changedPaths) {
        if (!before.source().equals(after.source())
                || after.schemaVersion() != before.schemaVersion() + 1) {
            return false;
        }
        Map<String, Object> beforeValues = before.values();
        Map<String, Object> afterValues = after.values();
        List<String> beforeUntouched = untouchedPaths(beforeValues, changedPaths);
        List<String> afterUntouched = untouchedPaths(afterValues, changedPaths);
        if (!beforeUntouched.equals(afterUntouched)) {
            return false;
        }
        for (String path : beforeUntouched) {
            if (beforeValues.get(path) != afterValues.get(path)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> untouchedPaths(Map<String, Object> values,
                                               Set<String> changedPaths) {
        List<String> paths = new ArrayList<String>();
        for (String path : values.keySet()) {
            if (!changedPaths.contains(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static ConfigMigrationResult failure(ConfigDocument document, String id,
                                                 int suppliedVersion, String expected,
                                                 String action) {
        ConfigValidationIssue issue = new ConfigValidationIssue(
                id, ValidationSeverity.ERROR, document.source(), "schema-version",
                String.valueOf(suppliedVersion), expected, IMPACT, action);
        return ConfigMigrationResult.failure(
                ConfigValidationReport.of(Collections.singletonList(issue)));
    }

    private static final class Step {
        private final Set<String> changedPaths;
        private final ConfigMigration migration;

        private Step(Set<String> changedPaths, ConfigMigration migration) {
            this.changedPaths = changedPaths;
            this.migration = migration;
        }
    }
}
