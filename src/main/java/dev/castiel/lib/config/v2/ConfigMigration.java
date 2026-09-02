package dev.castiel.lib.config.v2;

import java.util.Set;

/**
 * One immutable, forward-only configuration schema migration.
 *
 * <p>A migration owns exactly the step from {@link #fromVersion()} to one
 * version higher. Implementations must not mutate the supplied document and
 * must return a new document. Arcade migrations should declare only schema
 * paths they own in {@link #changedPaths()}; unknown third-party keys must
 * never be declared or modified by an arcade migration.</p>
 */
public interface ConfigMigration {
    /**
     * Returns the schema version this migration accepts.
     *
     * @return the source version for this one-step migration
     */
    int fromVersion();

    /**
     * Returns every flat schema path this migration may add, replace, remove,
     * or reorder.
     *
     * @return owned paths, never null
     */
    Set<String> changedPaths();

    /**
     * Migrates one document by exactly one schema version.
     *
     * @param document immutable input document
     * @return a new document at the next schema version
     */
    ConfigDocument migrate(ConfigDocument document);
}
