# P002 — Deterministic additive merge

- Status: blocked
- Requirements: R-001, R-002
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P001

## Contract

- Goal: merge only missing template nodes at canonical sibling locations.
- Read before editing: `architecture.md#document-merge`, `decisions.md#d002--preserve-existing-nodes-insert-only-missing-template-nodes`.
- Allowed files: `src/main/java/dev/castiel/lib/config/versioned/YamlMerger.java`; `src/main/java/dev/castiel/lib/config/versioned/YamlDocument.java`; `src/test/java/dev/castiel/lib/config/versioned/YamlMergerTest.java`.
- Protected scope: parser policy, public API, persistence.
- Expected existing symbols: P001 `YamlDocument` and `YamlNodes`.
- Implementation: implement the exact recursive mapping/list and before-next/after-previous insertion algorithm; return a changed flag; never mutate template or replace an existing node.
- Tests: root/nested/section/list/multiple additions, custom values, unknown keys, mismatched types, comments/order, and second-merge no-op.
- Verification: focused tests pass.
- Runtime proof: not applicable; pure merge.
- Stop condition: update report/progress and stop.

## Acceptance checklist

- [ ] Every existing node remains authoritative
- [ ] Inserted nodes carry template comments/defaults
- [ ] Existing and unknown relative order is retained
- [ ] Second merge reports unchanged
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:

