# P001 — Strict comment-preserving YAML document

- Status: ready
- Requirements: R-002, R-009
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: none

## Contract

- Goal: add the isolated strict YAML node document and relocated dependency.
- Read before editing: `architecture.md#document-model`, `decisions.md#d001--private-snakeyaml-node-layer`, `testing.md#fixture-suite`.
- Allowed files: `build.gradle`; `src/main/java/dev/castiel/lib/config/versioned/ConfigException.java`; `src/main/java/dev/castiel/lib/config/versioned/YamlDocument.java`; `src/main/java/dev/castiel/lib/config/versioned/YamlNodes.java`; `src/test/java/dev/castiel/lib/config/versioned/YamlDocumentTest.java`.
- Protected scope: existing config APIs and all non-config packages.
- Expected existing symbols: `ConfigManager`, Java release 8 setup, `shadowJar` relocation block.
- Implementation: add/relocate SnakeYAML 2.2 exactly as architecture specifies; implement strict single-map parsing, safe limits, comments, deep node cloning, deterministic dump, Java-value conversion, and strict Bukkit conversion. Package-private helpers only.
- Tests: round-trip nested scalar/list/map and header/block/inline/footer comments; malformed, duplicate, tagged, empty, sequence-root and multi-document rejection; deep clone independence.
- Verification: `./gradlew test --tests '*YamlDocumentTest'` and `./gradlew verifyJava8Bytecode` pass.
- Runtime proof: not applicable; pure parser behavior.
- Stop condition: update report/progress and stop; never start P002.

## Acceptance checklist

- [ ] Baseline and expected symbols verified
- [ ] Parser never returns blank data after invalid input
- [ ] Comments/order/styles survive semantic round-trip
- [ ] Dependency is relocated and Java 8 compatible
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:

