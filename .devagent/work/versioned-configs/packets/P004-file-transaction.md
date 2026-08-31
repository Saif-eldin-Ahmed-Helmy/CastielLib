# P004 — Safe load and atomic file transaction

- Status: blocked
- Requirements: R-005, R-006, R-011
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P002, P003

## Contract

- Goal: orchestrate candidate construction and persistence without risking the original.
- Read before editing: `architecture.md#load-transaction`, `architecture.md#persistence`.
- Allowed files: `src/main/java/dev/castiel/lib/config/versioned/ConfigFileStore.java`; `src/main/java/dev/castiel/lib/config/versioned/VersionedConfigLoader.java`; `src/test/java/dev/castiel/lib/config/versioned/ConfigFileStoreTest.java`; `src/test/java/dev/castiel/lib/config/versioned/VersionedConfigLoaderTest.java`.
- Protected scope: public builder and `ConfigManager`.
- Expected existing symbols: P001-P003 document, merger and migration plan.
- Implementation: implement exact ordered transaction, target/resource containment, UTF-8, same-directory temp files, force/close, backup-before-replace, atomic move fallback, cleanup, no-op no-write, and exact fresh resource install.
- Tests: fresh, changed/no-op, backup bytes, invalid YAML, migration failure, conversion failure, mapper failure hook, newer version, escape paths, injected move failure through a package-private file-move seam, and repeated load byte equality.
- Verification: focused filesystem tests pass on Windows; original bytes survive every failure test.
- Runtime proof: JUnit temp filesystem is the required runtime proof.
- Stop condition: update report/progress and stop.

## Acceptance checklist

- [ ] Validation/mapping occurs before persistence
- [ ] Changed existing file has immediate `.bak`
- [ ] Fresh/no-op behavior matches architecture
- [ ] Failures retain original and clean temps
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:
