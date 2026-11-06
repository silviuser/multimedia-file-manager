---
task: location-management
artifact: critique
round: 1
plan_version: 1
verdict: approved
---

# Plan critique: location-management, round 1

## Checklist

| Check | Result | Notes |
| --- | --- | --- |
| Every requirement in `scope.md` is covered | pass | `R1`–`R2` covered in Step 1 (`LocationRepository`); `R3`–`R6` covered in Step 2 (`MultimediaCatalog`); all mapped to tests `T9`–`T16` in Step 3. |
| One logical increment; existing behavior keeps working | pass | Adds storage and catalog location management while keeping existing `T1`–`T8` foundation tests intact (`TEST-6`). |
| Within the increment budget (files, lines, dependencies) | pass | 2 new production files (`LocationRepository.java`, `MultimediaCatalog.java`), ~245 production lines (<= 300), 0 runtime dependencies (`JAVA-1`). |
| Testable: essential tests mapped to requirements, commands given | pass | 8 isolated `Files.createTempDirectory` unit tests (`T9`–`T16`) cover success and error/safety paths for `R1`–`R6`; verification commands specified. |
| Concise: under about 200 lines, snippets only for critical parts | pass | 139 lines total; snippets limited to atomic `.tmp` move fallback and directory validation guards. |
| Follows `docs/standards/` (rule IDs) | pass | Complies with `JAVA-1`, `JAVA-2`, `JAVA-4`–`JAVA-9`, `DATA-1`–`DATA-5`, `DATA-7`, `VAL-1`–`VAL-3`, `VAL-5`, and `TEST-1`–`TEST-6`. |
| Consistent with `docs/ARCHITECTURE.md` | pass | Matches component locations, package structure, exception hierarchy (`D7`), and dual-sync persistence (`D2`, `D3`, `D5`). |
| No over-engineering | pass | Uses standard `java.nio.file` APIs and a focused `LoadResult` record without unnecessary layers or abstractions. |
| Placeholders recorded, with what removes them | pass | `TODO(TD-location-management-1)` documented for deferred media file scanning and statistics/report integration (`multimedia-scanner-filter`, `statistics-and-reports`). |
| Snippets correct and secure | pass | Sibling `.tmp` write handles `AtomicMoveNotSupportedException`, cleans up `.tmp` on failure with `addSuppressed`, and never modifies user media directories (`VAL-5`, `DATA-4`, `DATA-5`). |
| Risks identified | pass | Covers `AtomicMoveNotSupportedException` on WSL/Windows mounts and cross-platform `InvalidPathException` test input (`\0`). |

## Required changes

None.

## Suggestions

- Ensure `LocationRepository.LoadResult` record components have `@param` tags in the record's class-level JavaDoc so `javadoc -Xdoclint:all -Werror` in `./scripts/lint.sh` passes without warnings (`JAVA-8`, `JAVA-9`).

## Decision

Approved: development can start.
