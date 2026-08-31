# Implementation Architecture

- Revision: A001
- Planning baseline: `edb54545578317d7c77e144679eb356b0eb07e70` plus unrelated dirty changes present on 2026-08-31

## protected-scope

- Requirements: R-010, R-012
- Add the new system under `dev.castiel.lib.config.versioned`; only add a
  factory method to the existing `ConfigManager`.
- Do not change `ConfigManager.load`, `yaml`, `save`, `ConfigNode`, consumers,
  inventories, models, or plugin lifecycle code.
- No file watcher, annotation mapper, schema language, migration discovery,
  dependency injection layer, or cross-file transaction coordinator.

## document-model

- Requirements: R-002, R-009
- Add private `YamlDocument` and `YamlNodes` classes backed by relocated
  SnakeYAML 2.2 representation nodes. Parsing uses `LoaderOptions` with comment
  processing, duplicate keys rejected, recursive keys rejected, safe tag
  inspection, bounded aliases/depth/code points, and exactly one mapping-root
  document. Empty, multi-document, non-mapping, duplicate-key, unsafe-tag, and
  malformed inputs fail with `ConfigException` containing file and YAML mark.
- Dump with comments enabled, block style, two-space indentation, no line
  splitting, UTF-8 and `\n`. Preserve scalar styles and comment lists carried by
  nodes. Do not expose SnakeYAML types publicly.
- `YamlDocument.toBukkit()` dumps the candidate and calls
  `new YamlConfiguration().loadFromString(...)`; never use Bukkit's static
  loader because it logs parse errors and returns an empty config.

## document-merge

- Requirements: R-001, R-002
- Merge recursively only when both user and template values are mappings.
  Existing scalars, mappings, sequences, nulls, styles, comments and unknown
  keys always win. Sequences are atomic: a missing list is cloned from the
  template; an existing list is never element-merged.
- For each missing template tuple, deep-clone its full node subtree and comments.
  Insert it immediately before the next template sibling already present in the
  user mapping; if none, immediately after the nearest preceding template
  sibling; if no template sibling exists, append. Process template order from
  first to last. Never reorder an existing tuple. This keeps unknown keys and
  existing relative order while placing additions in the template's logical
  neighborhood.
- Mapping keys must be scalar strings. Dotted public paths address nested maps;
  literal dots in keys are unsupported and documented.
- A no-change load never dumps or writes, providing byte-for-byte stability.
  An actual change may normalize whitespace/quoting, but must retain user and
  template comments, mapping order, scalar style where supported, header/footer,
  and all values semantically.

## migrations-and-versions

- Requirements: R-003, R-004
- Reserved root key is `config-version`, a positive integer. Every bundled
  canonical resource must contain it and it must equal the builder's current
  version. CastielLib owns reading and updating this key.
- API registration is `migration(int fromVersion, ConfigMigration migration)`;
  each migration advances exactly `fromVersion -> fromVersion + 1`. Reject
  duplicate `fromVersion`, gaps required by the loaded file, versions below 1,
  and migrations at/above current during builder validation.
- `MutableConfig` exposes only `contains(path)`, `get(path)`, `set(path, value)`,
  and `remove(path)`. Values are null/scalars/lists/string-keyed maps; reject
  unsupported objects with the exact path. `get` returns detached Java values;
  `set` converts and deep-copies. Migration authors explicitly remove old keys.
- State handling:
  - absent file: install the bundled resource exactly; it is already current;
  - version equals current: skip migrations, still run additive merge;
  - older: require every sequential migration, applying in memory, and update
    `config-version` only after each migration returns successfully;
  - newer: fail without write;
  - missing version: fail unless `legacyVersion(int)` is declared; that explicit
    version becomes the starting version, then normal sequencing applies.
- A migration exception is wrapped with file and `vN -> vN+1`; no candidate is
  persisted.

## load-transaction

- Requirements: R-005, R-011
- `VersionedConfigBuilder.load()` delegates to the identity mapper and returns a
  detached `FileConfiguration`. `load(ConfigMapper<T>)` executes in this order:
  validate builder/resource, read strict user/template documents, resolve
  version, migrate candidate, merge missing defaults, set/check current version,
  convert candidate to Bukkit, invoke mapper, then persist only if bytes changed.
- `ConfigMapper<T>` is the domain boundary: it validates and constructs the
  immutable runtime object. It may throw `ConfigValidationException.at(path,
  reason)`. Wrap messages as `<file>: <path>: <reason>` without replacing bad
  existing data. A null mapper result is an error.
- Work entirely on an in-memory candidate. Do not mutate a previously returned
  Bukkit configuration.

## persistence

- Requirements: R-006
- Resolve resource through `JavaPlugin.getResource(resourceName)` and target
  beneath `plugin.getDataFolder()`. Reject absolute names, `..`, empty segments,
  directories, missing resources, or targets escaping the data folder.
- Read/write UTF-8. For a fresh file, atomically install exact resource bytes and
  do not create a backup. For an existing changed file, write candidate to a
  same-directory temp file, force/close it, create/replace `<name>.bak` through
  its own temp-and-move from the untouched original, then move candidate over
  target with `ATOMIC_MOVE + REPLACE_EXISTING`; fall back to `REPLACE_EXISTING`
  when atomic move is unsupported. Clean temp files in `finally`.
- If parsing, migration, Bukkit conversion, mapping/validation, backup, or write
  fails, report the operation/path and retain the original target. Backup is the
  immediate pre-change file and is created only for a persisted change.

## public-api

- Requirements: R-007
- Exact API:

```java
VersionedConfigBuilder versioned(String fileName, String resourceName, int currentVersion)

VersionedConfigBuilder legacyVersion(int version)
VersionedConfigBuilder migration(int fromVersion, ConfigMigration migration)
FileConfiguration load()
<T> T load(ConfigMapper<T> mapper)

interface ConfigMigration { void migrate(MutableConfig config) throws Exception; }
interface ConfigMapper<T> { T map(FileConfiguration config) throws Exception; }
interface MutableConfig {
  boolean contains(String path);
  Object get(String path);
  void set(String path, Object value);
  void remove(String path);
}
```

- `ConfigValidationException.at(String path, String reason)` is the standard
  domain diagnostic. `ConfigException` is the single runtime failure returned by
  loading, retaining operation, file and cause. All public types have concise
  Javadoc.
- Example:

```java
PluginConfig next = lib.configs()
    .versioned("config.yml", "config.yml", 3)
    .legacyVersion(1)
    .migration(1, yaml -> yaml.set("generator.period", yaml.get("generator.interval")))
    .migration(2, yaml -> yaml.remove("generator.interval"))
    .load(PluginConfig::from);
```

## reload-contract

- Requirements: R-008
- CastielLib prepares, validates, maps, and persists one file before returning.
  The consuming plugin loads every required file into local variables, builds one
  immutable aggregate snapshot, and assigns its single `volatile`/atomic active
  reference only after every call succeeds. On failure it catches
  `ConfigException`, logs it, and leaves the previous reference untouched.
- CastielLib does not own consumer runtime state and therefore must not provide a
  mutable live-config registry. Multiple files have independent schema versions.

## compatibility

- Requirements: R-009, R-010
- Add `implementation "org.yaml:snakeyaml:2.2"`; relocate
  `org.yaml.snakeyaml` to `dev.castiel.lib.libs.snakeyaml` in `shadowJar` to avoid
  server-provided-version conflicts. SnakeYAML 2.2 and all new code must pass the
  existing Java 8 bytecode and Spigot 1.8 linkage checks.
- Never call comment APIs added to modern Bukkit; Bukkit is only the validated
  read view handed to consumers.
