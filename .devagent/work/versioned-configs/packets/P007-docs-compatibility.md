# P007 — Documentation and compatibility proof

- Status: blocked
- Requirements: R-007, R-008, R-009, R-010, R-012
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P006

## Contract

- Goal: document minimal adoption/reload and prove distributable compatibility.
- Read before editing: `architecture.md#public-api`, `architecture.md#reload-contract`, `testing.md#compatibility-proof`.
- Allowed files: `docs/CONFIG.md`; `README.md`; `src/test/java/dev/castiel/lib/compatibility/Java8CompatibilityTest.java`; `src/test/java/dev/castiel/lib/compatibility/Spigot18LinkageTest.java`; `src/test/java/dev/castiel/lib/compatibility/VersionedConfigPackagingTest.java`.
- Protected scope: no consuming plugin or production behavior changes.
- Expected existing symbols: final P005 API and current compatibility tests.
- Implementation: retain legacy config docs, add recommended versioned API, schema key, legacy policy, migration example, before/after additive example, validation diagnostic, multiple-file use, and local-variable/immutable-snapshot reload pattern. Add packaging assertions for relocated SnakeYAML and new public linkage.
- Tests: compatibility and shadow-jar packaging assertions.
- Verification: `./gradlew clean check shadowJar`; inspect jar has relocated classes and no original SnakeYAML path.
- Runtime proof: built shadow jar plus Gradle reports.
- Stop condition: update report/progress and stop for senior review.

## Acceptance checklist

- [ ] Minimal API and before/after example documented
- [ ] Reload retains known-good snapshot on failure
- [ ] Java 8 bytecode and Spigot 1.8 linkage pass
- [ ] SnakeYAML is relocated in distributable jar
- [ ] No consuming plugin was touched

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:
