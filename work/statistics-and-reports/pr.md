---
task: statistics-and-reports
artifact: pr
branch: statistics-and-reports
base: main
updated_commit: none
pr: not created
---

# feat(service,storage): add primitive array statistics calculator, report exporter, and binary catalog storage

## Summary

Adds `StatisticsCalculator` to compute 1D (`int[] countsByExtension`, `long[] bytesByExtension`) and 2D (`long[][] directoryExtensionMatrix`) primitive array statistics from in-memory collections, `ReportExporter` to export formatted UTF-8 statistics and inventory reports to `reports/multimedia-report.txt`, and `BinaryCatalogStorage` to persist and restore binary catalog snapshots (`data/catalog.bin`) via `DataOutputStream` and `DataInputStream`. Integrates all three components into `MultimediaCatalog`, removing `TD-location-management-1`.

## Changes

- Created `StatisticsCalculator` (`project_ppoo/src/main/java/ro/ase/multimedia/service/StatisticsCalculator.java`) to compute 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive array statistics indexed by `MediaType.SUPPORTED_EXTENSIONS` and monitored directory order, wrapped in an immutable `StatisticsSnapshot` with deep defensive cloning.
- Created `ReportExporter` (`project_ppoo/src/main/java/ro/ase/multimedia/storage/ReportExporter.java`) to write formatted UTF-8 text reports (`multimedia-report.txt`) containing summary totals, monitored directories, 1D extension statistics, 2D directory-by-extension matrix, and the multimedia file inventory atomically via a sibling `.tmp` file.
- Created `BinaryCatalogStorage` (`project_ppoo/src/main/java/ro/ase/multimedia/storage/BinaryCatalogStorage.java`) to save and load full binary snapshots (`catalog.bin`) with magic/version validation (`0x4D464D43`, version `1`), monitored directories, discovered files, and 1D/2D primitive arrays via `DataOutputStream` and `DataInputStream`.
- Updated `MultimediaCatalog` (`project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`) to integrate `StatisticsCalculator`, `ReportExporter`, and `BinaryCatalogStorage`, automatically recalculate `StatisticsSnapshot` on lifecycle rescan events, expose `getStatistics()`, `exportReport()`, `saveBinarySnapshot()`, and `loadBinarySnapshot()`, persist both `locations.txt` and `catalog.bin` in `saveAllOnExit()`, and remove `TD-location-management-1`.
- Added unit tests `T22`–`T28` (`project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`) covering 1D/2D primitive array computation, defensive copy isolation, UTF-8 report export, binary snapshot round-trip persistence, corrupted file handling, and catalog integration.

## How to test

```bash
./scripts/build.sh
./scripts/lint.sh
./scripts/test.sh
```

- E1: Run `./scripts/run.sh` from the repository root and verify clean startup and exit (`0`) without leaving runtime persistence files.
- E2: Populate 3 monitored directories (`audio_dir`, `images_dir`, `missing_dir`), delete `missing_dir` on disk, call `catalog.rescan()`, inspect `catalog.getStatistics()`, and mutate returned `int[]`, `long[]`, and `long[][]` arrays to verify exact 1D/2D values and defensive copy immutability.
- E3: Call `ReportExporter.export(List.of(), List.of(), emptyStats)` to a nested temporary report path and verify the UTF-8 report formats empty sections as `(none)` and leaves no `.tmp` sibling file.
- E4: Write binary files with unsupported `FORMAT_VERSION = 999` and negative directory count `-5`, call `BinaryCatalogStorage.load()`, and verify `StorageOperationException` is thrown in both cases.
- E5: Instantiate `new MultimediaCatalog(new LocationRepository(tempRoot.resolve("locations.txt")))`, call `exportReport()` and `saveAllOnExit()`, and verify `reports/multimedia-report.txt` and `data/catalog.bin` are created under `tempRoot` and `loadBinarySnapshot()` restores all directories and files.

## Quality gates

| Gate | Task file | Result |
| --- | --- | --- |
| Plan critique | `critique.md`, round 1 | approved |
| Code review | `review.md`, round 1 | approved |
| Tests | `test-report.md`, round 1 | pass |

## Tech debt

- None (resolved `TD-location-management-1` in `MultimediaCatalog.java`)

## Follow-ups

- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.
- `javadoc-and-documentation`: complete JavaDoc verification, repeatable manual test scenarios, and course documentation.
