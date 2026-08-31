# Requirement Traceability

| ID | Source | Architecture owner | Packet(s) | Required evidence | Status |
| --- | --- | --- | --- | --- | --- |
| R-001 | Automatic additive evolution | `architecture.md#document-merge` | P002, P006 | merge fixtures | planned |
| R-002 | Preserve values, unknown keys, lists, order, comments | `architecture.md#document-merge` | P001, P002, P006 | round-trip fixtures | planned |
| R-003 | Explicit sequential migrations | `architecture.md#migrations-and-versions` | P003, P006 | migration tests | planned |
| R-004 | Fresh/current/old/newer/unversioned states | `architecture.md#migrations-and-versions` | P003, P005, P006 | state matrix tests | planned |
| R-005 | Validate before persistence; path diagnostics | `architecture.md#load-transaction` | P004, P005, P006 | failure tests | planned |
| R-006 | Atomic writes and backup without data loss | `architecture.md#persistence` | P004, P006 | filesystem tests | planned |
| R-007 | Minimal multi-file API | `architecture.md#public-api` | P005, P007 | compile/API tests | planned |
| R-008 | Immutable atomic runtime reload guidance | `architecture.md#reload-contract` | P005, P007 | mapper failure test + docs | planned |
| R-009 | Java 8 and legacy Bukkit compatibility | `architecture.md#compatibility` | P001, P007 | bytecode/linkage checks | planned |
| R-010 | Existing ConfigManager API remains compatible | `architecture.md#compatibility` | P005, P007 | existing suite | planned |
| R-011 | Idempotent startup and reload | `architecture.md#load-transaction` | P004, P006 | byte equality tests | planned |
| R-012 | No consuming-plugin port | `architecture.md#protected-scope` | all | diff inspection | planned |

