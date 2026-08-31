# P003 — Version state and migration path API

- Status: blocked
- Requirements: R-003, R-004
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P001

## Contract

- Goal: provide deterministic in-memory vN-to-vN+1 migrations.
- Read before editing: `architecture.md#migrations-and-versions`, `architecture.md#public-api`, `decisions.md#d003--explicit-legacy-baseline`.
- Allowed files: `src/main/java/dev/castiel/lib/config/versioned/ConfigMigration.java`; `src/main/java/dev/castiel/lib/config/versioned/MutableConfig.java`; `src/main/java/dev/castiel/lib/config/versioned/NodeMutableConfig.java`; `src/main/java/dev/castiel/lib/config/versioned/MigrationPlan.java`; `src/test/java/dev/castiel/lib/config/versioned/MigrationPlanTest.java`.
- Protected scope: merge, disk I/O, Bukkit conversion.
- Expected existing symbols: P001 node conversion/path helpers.
- Implementation: exact interfaces and state rules from architecture; positive integer versions; sequential registry; detached values; dotted paths; update version only after each success; wrap transition failures.
- Tests: v1-v2-v3, missing/duplicate/out-of-range migrations, newer/current, explicit/undeclared legacy, thrown migration, set/get/remove/contains and unsupported values.
- Verification: focused tests and Javadoc compilation pass.
- Runtime proof: not applicable; pure migration.
- Stop condition: update report/progress and stop.

## Acceptance checklist

- [ ] No version is inferred for unversioned files
- [ ] No transition can be skipped or duplicated
- [ ] Failed transition leaves source document untouched
- [ ] Public interfaces have concise Javadoc
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:

