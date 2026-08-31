# C010 — Evidence-only closure check

- Status: passed (evidence-only)
- Date: 2026-08-31
- Provenance: executed in the existing dirty checkout `G:/Projects/Commissions/CastielLib`; no source or test files were edited. The C010 packet was not present on disk, so the three reviewed P006 files were taken to be the v2 production files listed below. This scope assumption is recorded rather than inferred as historical fact.

## Integrity evidence

| Reviewed file | Pre SHA-256 | Fresh-copy SHA-256 | Post SHA-256 |
| --- | --- | --- | --- |
| `src/main/java/dev/castiel/lib/config/v2/ConfigKey.java` | `6ED712EEB0F71E2BF4EDE204D86C4739DEB68E58AAED10EE3302CD61F299BAF2` | `6ED712EEB0F71E2BF4EDE204D86C4739DEB68E58AAED10EE3302CD61F299BAF2` | `6ED712EEB0F71E2BF4EDE204D86C4739DEB68E58AAED10EE3302CD61F299BAF2` |
| `src/main/java/dev/castiel/lib/config/v2/ConfigValidationIssue.java` | `06EDBA06AB17EAC747847F7BAF791886B6C827F5FB336B22481733313CE12208` | `06EDBA06AB17EAC747847F7BAF791886B6C827F5FB336B22481733313CE12208` | `06EDBA06AB17EAC747847F7BAF791886B6C827F5FB336B22481733313CE12208` |
| `src/main/java/dev/castiel/lib/config/v2/ConfigValidationReport.java` | `B58DC356B116578F8EEA6C853D0478C774B090A52A57D1FCAAF344AD18545554` | `B58DC356B116578F8EEA6C853D0478C774B090A52A57D1FCAAF344AD18545554` | `B58DC356B116578F8EEA6C853D0478C774B090A52A57D1FCAAF344AD18545554` |

The preserved exact predicate/assertion snippet in `ConfigKeyTest` is unchanged before/after:

```java
ConfigKey<Integer> key = new ConfigKey<Integer>(
        "storage.pool-size", Integer.class, 4, value -> value.intValue() >= 1,
        EXPECTED, IMPACT, ACTION, false);
assertEquals("config.value.invalid", issue.id());
```

## Verification

Command: `./gradlew.bat test --tests dev.castiel.lib.config.v2.ConfigKeyTest verifyJava8Bytecode`

Result: `BUILD SUCCESSFUL`; `verifyJava8Bytecode` passed. Fresh XML was parsed from `build/test-results/test/TEST-dev.castiel.lib.config.v2.ConfigKeyTest.xml`: `tests=7`, `failures=0`, `errors=0`, `skipped=0`.

Cleanup: temporary fresh-copy directory removed; `CLEANUP_EXISTS=False`, `CLEANUP_REMAINING=0`.

No production or test source changes were made. Only this evidence report and `progress.md` were updated.
