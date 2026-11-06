---
task: location-management
artifact: pr
branch: location-management
base: main
updated_commit: none
pr: https://github.com/silviuser/multimedia-file-manager/pull/2
---

# feat(location-management): implement monitored location persistence and catalog directory management

## Summary

Adds `LocationRepository` in `ro.ase.multimedia.storage` to persist monitored directory paths in `locations.txt` (`UTF-8`) using atomic sibling `.tmp` writes and non-fatal startup warning collection. Adds `MultimediaCatalog` in `ro.ase.multimedia.service` to manage the in-memory `List<MonitoredDirectory>` and `Set<Path>` collections across startup restoration, status classification (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), validated addition, non-destructive reference removal, and shutdown persistence.

## Changes

- `project_ppoo/src/main/java/ro/ase/multimedia/storage/LocationRepository.java`: implements `LocationRepository` and `LoadResult` for UTF-8 loading, missing-file initialization, malformed-line warning collection, and atomic `.tmp` saving (`R1`, `R2`).
- `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`: implements `MultimediaCatalog` managing `List<MonitoredDirectory>` and `Set<Path>` for startup deduplication and status classification, validated directory addition, non-destructive reference removal (`S3`), status refresh, and exit persistence (`R3`–`R6`).
- `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: adds recursive temp-directory cleanup helper and 8 isolated temporary-directory unit tests (`T9`–`T16`) covering `R1`–`R6` while keeping `T1`–`T8` passing.

## How to test

```bash
./scripts/build.sh
./scripts/lint.sh
./scripts/test.sh
```

- Run `./scripts/run.sh` to verify the console entry point starts cleanly and exits `0`.
- All 16 automated tests (`T1`–`T16`) execute in isolated `Files.createTempDirectory` folders and clean up temporary files automatically (`E1`–`E4` in `work/location-management/test-report.md`).

## Quality gates

| Gate | Task file | Result |
| --- | --- | --- |
| Plan critique | `critique.md`, round 1 | approved |
| Code review | `review.md`, round 1 | approved |
| Tests | `test-report.md`, round 1 | pass |

## Tech debt

- `TD-location-management-1`: Media file scanning (`MultimediaScanner`), 1D/2D array recalculation (`StatisticsCalculator`), and binary/report persistence (`BinaryCatalogStorage`, `ReportExporter`) in `MultimediaCatalog.java:24` are deferred to `multimedia-scanner-filter` and `statistics-and-reports`.

## Follow-ups

- `multimedia-scanner-filter`: `MultimediaScanner` and catalog media file collections/filtering.
- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.
