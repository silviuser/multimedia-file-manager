---
task: statistics-and-reports
artifact: dev-log
plan_version: 1
round: 1
status: done
---

# Development log: statistics-and-reports

## Increment 1: 1. Create `StatisticsCalculator` and `StatisticsSnapshot` (`R1`, `JAVA-2`, `JAVA-3`, `JAVA-7`)

- Commit: `f2aaa2e` feat(service): add primitive array statistics calculator
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/service/StatisticsCalculator.java`: created pure domain service computing 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive arrays indexed by `MediaType.SUPPORTED_EXTENSIONS` and monitored directory order, wrapped in immutable `StatisticsSnapshot` with deep defensive cloning.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added unit tests `T22` and `T23` verifying 1D/2D array calculations, defensive copy isolation, empty inputs, and null argument validation.
- Decisions: Used `putIfAbsent` when indexing monitored directory paths so duplicate entries in raw input lists map consistently to the first matching row index.
- Tests: `T22` checks 1D extension counts/bytes, 2D directory-by-extension matrix values, and defensive cloning on getters; `T23` checks empty lists and null/invalid dimension rejection.
- Review focus: Deep cloning of `long[][]` in both `StatisticsSnapshot` constructor and `getDirectoryExtensionMatrix()`.
- Deviation from the plan: none

## Increment 2: 2. Create `ReportExporter` (`R2`, `JAVA-6`, `DATA-1`, `DATA-4`, `DATA-7`, `VAL-1`)

- Commit: `ac8dbf1` feat(storage): add text report exporter
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/storage/ReportExporter.java`: created UTF-8 text report exporter writing summary totals, monitored directories, 1D extension statistics, 2D directory-by-extension matrix, and multimedia file inventory atomically via a sibling `.tmp` file.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added unit tests `T24` and `T25` checking formatted report output, atomic write cleanup, `StorageOperationException` on write failure, and null argument validation.
- Decisions: Formatted empty directory/file sections as `(none)` for clear human-readable reports even when the catalog is empty.
- Tests: `T24` checks UTF-8 report sections, 1D/2D values, and `.tmp` cleanup; `T25` checks `StorageOperationException` when target path is a directory and `IllegalArgumentException` on null arguments.
- Review focus: Atomic sibling `.tmp` write with `ATOMIC_MOVE` fallback to `REPLACE_EXISTING` and temporary file cleanup in the `catch (IOException)` block.
- Deviation from the plan: none

## Increment 3: 3. Create `BinaryCatalogStorage` (`R3`, `JAVA-6`, `DATA-1`, `DATA-6`, `DATA-7`, `VAL-1`)

- Commit: `148629f` feat(storage): add binary catalog storage
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/storage/BinaryCatalogStorage.java`: created binary catalog snapshot storage (`MAGIC_HEADER = 0x4D464D43`, `FORMAT_VERSION = 1`) persisting and restoring monitored directories, multimedia files, and 1D/2D primitive arrays via `DataOutputStream`/`DataInputStream`.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added unit tests `T26` and `T27` checking binary round-trip save/load, missing file handling (`Optional.empty()`), corrupted header/stream detection, and write failure cleanup.
- Decisions: Validated non-negative counts and exact `SUPPORTED_EXTENSIONS.size()` array dimensions before allocating arrays during `load()` to prevent invalid allocations on corrupted streams.
- Tests: `T26` checks missing file empty result and full round-trip restoration of directories, files, and 1D/2D statistics arrays; `T27` checks `StorageOperationException` on bad magic header, truncated `EOFException`, and directory write target.
- Review focus: Stream framing and bounds validation in `BinaryCatalogStorage.load()`.
- Deviation from the plan: Caught `IOException | IllegalArgumentException` instead of `IOException | IllegalArgumentException | InvalidPathException` because `InvalidPathException` is a subclass of `IllegalArgumentException` in the standard library and Java forbids related types in a multi-catch clause; behavior is identical.

## Increment 4: 4. Integrate into `MultimediaCatalog` and 5. Add unit tests in `TestRunner` (`R4`, `DATA-2`, `DATA-3`, `DATA-7`, `TEST-1`–`TEST-6`)

- Commit: `d90be72` feat(service): integrate statistics calculator, report exporter and binary storage into catalog
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`: removed `TODO(TD-location-management-1)`, added 5-arg constructor and default path resolution relative to `locationRepository.getLocationsFile().getParent()`, initialized `currentStatistics` in constructor, updated `rescan()` and `saveAllOnExit()`, and added `getStatistics()`, `exportReport()`, `saveBinarySnapshot()`, and `loadBinarySnapshot()`.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added unit test `T28` verifying catalog statistics recalculation across `initialize()`, `addDirectory()`, `removeDirectory()`, `exportReport()`, and `saveAllOnExit()` / `loadBinarySnapshot()`.
- Decisions: Followed critique suggestion to initialize `currentStatistics` in the `MultimediaCatalog` constructor so `getStatistics()`, `exportReport()`, and `saveBinarySnapshot()` never encounter a `null` snapshot before `initialize()`.
- Tests: `T28` checks lifecycle statistics updates, report export, and binary snapshot persistence on exit.
- Review focus: Default path resolution in `MultimediaCatalog` 2-arg constructor (`resolveBaseDir`) keeping `T11`–`T21` isolated in temporary directories.
- Deviation from the plan: none

## Commands

| Command | Result |
| --- | --- |
| build: `./scripts/build.sh` | 0 errors |
| test: `./scripts/test.sh` | 28 passed, 0 failed |
| lint: `./scripts/lint.sh` | no problems |

## Notes for testing

- Run `./scripts/test.sh` and `./scripts/lint.sh` to verify all 28 automated unit tests (`T1`–`T28`) and zero compiler/Javadoc warnings.
- `MultimediaCatalog` now maintains live 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive array statistics via `getStatistics()`, writes UTF-8 reports via `exportReport()`, and persists binary snapshots (`catalog.bin`) on `saveAllOnExit()`.
