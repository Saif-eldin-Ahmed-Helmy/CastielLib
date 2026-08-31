# Decisions

## D001 — Private SnakeYAML node layer

- Context: Bukkit static loading hides invalid YAML and Bukkit comment behavior
  differs by server version; current SnakeYAML Engine requires Java 11.
- Options considered: Bukkit defaults/save, text insertion, SnakeYAML Engine,
  classic SnakeYAML representation nodes.
- Decision: pin and relocate classic SnakeYAML 2.2, use its node/comment API.
- Consequences: one small shaded dependency and normalized formatting only when
  a write is necessary; safe Java 8 behavior and no Bukkit-version dependence.

## D002 — Preserve existing nodes; insert only missing template nodes

- Context: user values and unknown keys are authoritative while additions need
  canonical location/comments.
- Options considered: template-first rebuild, user-first append, anchored tuple
  insertion.
- Decision: anchored insertion without reordering existing tuples; recurse maps,
  treat lists atomically.
- Consequences: minimal semantic change and deterministic idempotence.

## D003 — Explicit legacy baseline

- Context: an unversioned file's schema cannot be inferred safely.
- Options considered: assume v1, treat as current, require declaration.
- Decision: require `legacyVersion`; otherwise fail unchanged.
- Consequences: one plugin-side declaration for legacy adoption, no destructive
  guessing.

## D004 — Mapper is validation and immutable-construction boundary

- Context: generic evolution cannot validate materials/actions/rewards and reload
  must not mutate live state progressively.
- Options considered: separate validators and mappers, typed reflection mapping,
  one mapper callback.
- Decision: one `ConfigMapper<T>` callback validates and constructs a candidate.
- Consequences: smaller API; path-specific `ConfigValidationException` remains
  available and persistence happens only after mapping succeeds.

## D005 — Per-file persistence, consumer-owned snapshot swap

- Context: configs have independent versions; CastielLib cannot know a plugin's
  aggregate runtime type.
- Options considered: global config registry/transaction, per-file loader.
- Decision: per-file safe load; consumer builds all locals then replaces one
  active immutable snapshot.
- Consequences: no overbuilt cross-file coordinator. A later file failure may
  leave an earlier file safely evolved on disk, but live state remains unchanged.

## D006 — One immediate backup

- Context: every changed existing file needs recovery without unbounded backup
  accumulation.
- Options considered: timestamp history, no backup for additive merges, `.bak`.
- Decision: atomically replace `<file>.bak` with the immediate pre-change bytes.
- Consequences: bounded disk usage and simple recovery.

