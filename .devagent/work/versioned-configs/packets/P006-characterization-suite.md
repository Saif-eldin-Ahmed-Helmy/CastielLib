# P006 — Complete characterization suite

- Status: blocked
- Requirements: R-001 through R-011
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P005

## Contract

- Goal: close every required behavior with real YAML fixtures and integration tests.
- Read before editing: `testing.md#fixture-suite`, `requirements.md`.
- Allowed files: `src/test/java/dev/castiel/lib/config/versioned/VersionedConfigCharacterizationTest.java`; `src/test/resources/versioned-config/**`; existing P001-P005 test files only for missing assertions.
- Protected scope: production code unless a test proves a contradiction; if found, stop and request a correction packet rather than expanding scope.
- Expected existing symbols: completed P001-P005 API.
- Implementation: create compact named fixtures and cover every case listed in `testing.md`, including two files and byte/idempotence assertions. Do not duplicate lower-level assertions unnecessarily.
- Tests: the complete matrix in `testing.md#fixture-suite`.
- Verification: `./gradlew test verifyJava8Bytecode` passes twice consecutively.
- Runtime proof: test report is sufficient; no live server needed.
- Stop condition: update report/progress and stop.

## Acceptance checklist

- [ ] Every R-001..R-011 row has evidence
- [ ] Failure fixtures prove original-byte preservation
- [ ] Comment/order and idempotence use byte/position assertions
- [ ] Entire existing suite remains green
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:

