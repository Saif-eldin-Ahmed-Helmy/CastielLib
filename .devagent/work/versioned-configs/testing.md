# Testing and Evidence Plan

## fixture-suite

- Requirements: R-001 through R-011
- Unit proof: JUnit 5 temp-directory tests using real YAML strings/resources.
  Cover fresh exact copy; current no-op byte equality; root/nested/section/list
  additions; multiple additions; changed default preserving custom value;
  unknown keys; scalar/list/nested values; header/block/inline/footer comments and
  anchored insertion order; explicit unversioned adoption and undeclared failure;
  sequential v1-v2-v3; missing/duplicate migration; throwing migration; malformed,
  duplicate-key, unsafe-tag, empty, non-map and multi-document YAML; mapper path
  failure; newer version; multiple independent files; backup bytes; repeated
  startup/reload byte equality; unsupported migration value/path.
- Integration proof: builder through a minimal test `JavaPlugin` fixture with
  resource and data folders; do not require a running server.
- Commands: `./gradlew test verifyJava8Bytecode`
- Pass criteria: all cases pass, existing tests remain green, no unchanged file
  mtime/content mutation, failed operations preserve original bytes.
- Runtime decision: no live server justified; file transaction and Spigot 1.8
  linkage are covered deterministically by tests.

## compatibility-proof

- Requirements: R-009, R-010
- Unit proof: extend existing Java 8 compatibility/linkage assertions to include
  new public types and relocated dependency classes in the shadow jar.
- Commands: `./gradlew clean check shadowJar`
- Pass criteria: class major <= 52, Spigot 1.8 linkage passes, shadow jar contains
  relocated SnakeYAML and no `org/yaml/snakeyaml` entries.
- Artifact location: Gradle reports and `build/libs` (not committed).

