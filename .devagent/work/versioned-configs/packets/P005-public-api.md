# P005 — Minimal ConfigManager public API

- Status: blocked
- Requirements: R-004, R-005, R-007, R-008, R-010
- Architecture revision: A001
- Planning baseline: current HEAD plus pre-existing dirty worktree
- Depends on: P004

## Contract

- Goal: expose the final builder/mapper/diagnostic API without changing old behavior.
- Read before editing: `architecture.md#public-api`, `architecture.md#reload-contract`, `decisions.md#d004--mapper-is-validation-and-immutable-construction-boundary`.
- Allowed files: `src/main/java/dev/castiel/lib/config/ConfigManager.java`; `src/main/java/dev/castiel/lib/config/versioned/VersionedConfigBuilder.java`; `src/main/java/dev/castiel/lib/config/versioned/ConfigMapper.java`; `src/main/java/dev/castiel/lib/config/versioned/ConfigValidationException.java`; `src/test/java/dev/castiel/lib/config/versioned/VersionedConfigBuilderTest.java`.
- Protected scope: signatures/behavior of existing `ConfigManager` methods and `ConfigNode`.
- Expected existing symbols: P004 loader and existing `ConfigManager(JavaPlugin)`.
- Implementation: add exact factory and fluent methods, eager argument/registration checks, identity `load()`, mapper-based `load`, null-result rejection, and file/path diagnostic formatting. Freeze/copy builder registration before load; builder need not be thread-safe.
- Tests: API compile/use, resource-version mismatch, all state cases, mapper validation path, mapper immutable object, multiple filenames, and legacy methods unchanged.
- Verification: focused tests and existing config-related tests pass.
- Runtime proof: not applicable beyond filesystem integration in P004.
- Stop condition: update report/progress and stop.

## Acceptance checklist

- [ ] Plugin use is one factory chain plus load
- [ ] Domain invalid values are never defaulted over
- [ ] Diagnostics are `<file>: <path>: <reason>`
- [ ] Existing APIs remain source/binary compatible
- [ ] Only allowed files changed

## Execution report

- Changed files:
- Commands/results:
- Evidence:
- Contradictions/local fixes:
- Remaining risks:

