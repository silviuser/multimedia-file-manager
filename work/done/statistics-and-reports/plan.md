---
task: statistics-and-reports
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Primitive Array Statistics Calculator, Text Report Exporter, and Binary Catalog Storage

## Summary

Implement `StatisticsCalculator` to compute 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive array statistics over monitored directories and multimedia files, `ReportExporter` to write UTF-8 text reports (`multimedia-report.txt`), and `BinaryCatalogStorage` to persist and restore binary snapshots (`catalog.bin`) via `DataOutputStream`/`DataInputStream`. Integrate all three components into `MultimediaCatalog`, removing `TD-location-management-1`.

## Context

- `docs/ARCHITECTURE.md`: defines `StatisticsCalculator`, `ReportExporter`, `BinaryCatalogStorage`, and `MultimediaCatalog` responsibilities (`G1`–`G4`, `S4`, `D4`, `D8`).
- `docs/standards/java.md` & `data.md`: `JAVA-2` (defensive copies), `JAVA-3` (1D `int[]`/`long[]` and 2D `long[][]`), `JAVA-6` (`try-with-resources`), `JAVA-7` (pure calculator), `DATA-1`–`DATA-7` (atomic sibling `.tmp` writes, `DataOutputStream`/`DataInputStream`, configurable paths).
- `MultimediaCatalog.java`: already calls `rescan()` at the end of `initialize()`, `addDirectory()`, and `removeDirectory()`, making `rescan()` the single recalculation point for `StatisticsSnapshot`.

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `project_ppoo/src/main/java/ro/ase/multimedia/service/StatisticsCalculator.java` | create | Pure service computing 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive arrays wrapped in `StatisticsSnapshot` |
| 2 | `project_ppoo/src/main/java/ro/ase/multimedia/storage/ReportExporter.java` | create | Exports formatted UTF-8 statistics and file inventory reports atomically to `multimedia-report.txt` |
| 3 | `project_ppoo/src/main/java/ro/ase/multimedia/storage/BinaryCatalogStorage.java` | create | Saves and loads binary catalog and primitive array snapshots (`catalog.bin`) using `DataOutputStream`/`DataInputStream` |
| 4 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java` | modify | Integrates calculator, report exporter, and binary storage; updates `rescan()` and `saveAllOnExit()`; resolves `TD-location-management-1` |
| 5 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | modify | Adds automated unit tests `T22`–`T28` covering `R1`–`R4` and error cases |

## Steps

### Step 1. Create `StatisticsCalculator` and `StatisticsSnapshot` (`R1`, `JAVA-2`, `JAVA-3`, `JAVA-7`)

Create `ro.ase.multimedia.service.StatisticsCalculator`:
- Define `calculate(List<MonitoredDirectory> directories, List<MultimediaFile> files)` that validates non-null lists and elements (`IllegalArgumentException`).
- Index extensions by `MediaType.SUPPORTED_EXTENSIONS` (`[0]=".mp3"`, `[1]=".wav"`, `[2]=".jpg"`, `[3]=".png"`) and directories by their list order in `directories`.
- Populate `int[] countsByExtension`, `long[] bytesByExtension`, and `long[][] directoryExtensionMatrix` (`[dirIndex][extIndex]` file counts where `dir.getPath().equals(file.getDirectoryPath())`).
- Return an immutable `StatisticsSnapshot` that deep-clones the 1D and 2D arrays in both its constructor and getters (`getCountsByExtension()`, `getBytesByExtension()`, `getDirectoryExtensionMatrix()`, `getTotalFiles()`, `getTotalBytes()`).

```java
for (MultimediaFile file : files) {
    int extIndex = MediaType.SUPPORTED_EXTENSIONS.indexOf(file.getExtension());
    if (extIndex >= 0) {
        countsByExtension[extIndex]++;
        bytesByExtension[extIndex] += file.getSizeBytes();
        Integer dirIndex = directoryIndices.get(file.getDirectoryPath());
        if (dirIndex != null) {
            directoryExtensionMatrix[dirIndex][extIndex]++;
        }
    }
}
```

### Step 2. Create `ReportExporter` (`R2`, `JAVA-6`, `DATA-1`, `DATA-4`, `DATA-7`, `VAL-1`)

Create `ro.ase.multimedia.storage.ReportExporter`:
- Accept target `Path reportFile` in constructor (non-null, normalized absolute path) and expose `getReportFile()`.
- Implement `export(List<MonitoredDirectory> directories, List<MultimediaFile> files, StatisticsCalculator.StatisticsSnapshot statistics)`:
  - Validate non-null parameters (`IllegalArgumentException`).
  - Format sections: Summary Totals, Monitored Directories, 1D Extension Statistics (`.mp3`, `.wav`, `.jpg`, `.png` counts and byte totals), 2D Directory-by-Extension Matrix, and Multimedia File Inventory.
  - Write UTF-8 lines to `reportFile.resolveSibling(reportFile.getFileName() + ".tmp")` and move over `reportFile` with `ATOMIC_MOVE` fallback to `REPLACE_EXISTING`, cleaning up `.tmp` and throwing `StorageOperationException` on `IOException`.

### Step 3. Create `BinaryCatalogStorage` (`R3`, `JAVA-6`, `DATA-1`, `DATA-6`, `DATA-7`, `VAL-1`)

Create `ro.ase.multimedia.storage.BinaryCatalogStorage`:
- Define `MAGIC_HEADER = 0x4D464D43` (`"MFMC"`) and `FORMAT_VERSION = 1`.
- Implement `save(List<MonitoredDirectory> directories, List<MultimediaFile> files, StatisticsCalculator.StatisticsSnapshot statistics)` writing header, directories, files, `int[] countsByExtension`, `long[] bytesByExtension`, and `long[][] directoryExtensionMatrix` to a sibling `.tmp` file via `try (DataOutputStream out = ...)` and moving atomically over `binaryFile`.
- Implement `load()` returning `Optional.empty()` when `!Files.exists(binaryFile)`, or reading and validating the stream via `try (DataInputStream in = ...)` into `Optional<CatalogSnapshot>`. Wrap `IOException`, `IllegalArgumentException`, or `InvalidPathException` (corrupted header, negative counts, invalid enum/path) in `StorageOperationException`.

```java
try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(binaryFile)))) {
    int magic = in.readInt();
    int version = in.readInt();
    if (magic != MAGIC_HEADER || version != FORMAT_VERSION) {
        throw new StorageOperationException("Unsupported or corrupted binary catalog header in: " + binaryFile);
    }
    // Read directories, files, 1D int[], 1D long[], and 2D long[][]
} catch (IOException | IllegalArgumentException | InvalidPathException ex) {
    throw new StorageOperationException("Failed to load binary catalog from: " + binaryFile, ex);
}
```

### Step 4. Integrate into `MultimediaCatalog` (`R4`, `DATA-2`, `DATA-3`, `DATA-7`)

Update `ro.ase.multimedia.service.MultimediaCatalog`:
- Remove `TODO(TD-location-management-1)`.
- Add `StatisticsCalculator`, `BinaryCatalogStorage`, `ReportExporter`, and `StatisticsCalculator.StatisticsSnapshot currentStatistics` fields.
- Keep `MultimediaCatalog(LocationRepository)` and `MultimediaCatalog(LocationRepository, MultimediaScanner)` by deriving default paths relative to `locationRepository.getLocationsFile().getParent()` (`data/catalog.bin` and `reports/multimedia-report.txt`), and add `MultimediaCatalog(LocationRepository, MultimediaScanner, StatisticsCalculator, BinaryCatalogStorage, ReportExporter)`.
- Update `rescan()` to recompute `currentStatistics = statisticsCalculator.calculate(monitoredDirectories, multimediaFiles)`.
- Add `getStatistics()`, `exportReport()`, `saveBinarySnapshot()`, and `loadBinarySnapshot()`.
- Update `saveAllOnExit()` to refresh statuses, recalculate `currentStatistics`, save `locations.txt`, and call `binaryStorage.save(monitoredDirectories, multimediaFiles, currentStatistics)`.

### Step 5. Add unit tests in `TestRunner` (`TEST-1`–`TEST-6`)

Add tests `T22`–`T28` in `TestRunner.java` using temporary directories and verify `./scripts/test.sh` and `./scripts/lint.sh`.

## Interfaces and data

- `StatisticsCalculator`:
  - `public StatisticsSnapshot calculate(List<MonitoredDirectory> directories, List<MultimediaFile> files)`
  - `StatisticsSnapshot`: `int[] getCountsByExtension()`, `long[] getBytesByExtension()`, `long[][] getDirectoryExtensionMatrix()`, `int getTotalFiles()`, `long getTotalBytes()`
- `ReportExporter`:
  - `public ReportExporter(Path reportFile)`
  - `public Path getReportFile()`
  - `public void export(List<MonitoredDirectory> directories, List<MultimediaFile> files, StatisticsCalculator.StatisticsSnapshot statistics)`
- `BinaryCatalogStorage`:
  - `public BinaryCatalogStorage(Path binaryFile)`
  - `public Path getBinaryFile()`
  - `public void save(List<MonitoredDirectory> directories, List<MultimediaFile> files, StatisticsCalculator.StatisticsSnapshot statistics)`
  - `public Optional<CatalogSnapshot> load()`
  - `public record CatalogSnapshot(List<MonitoredDirectory> directories, List<MultimediaFile> files, StatisticsCalculator.StatisticsSnapshot statistics)`
- `MultimediaCatalog` additions:
  - `public MultimediaCatalog(LocationRepository, MultimediaScanner, StatisticsCalculator, BinaryCatalogStorage, ReportExporter)`
  - `public StatisticsCalculator.StatisticsSnapshot getStatistics()`
  - `public void exportReport()`
  - `public void saveBinarySnapshot()`
  - `public Optional<BinaryCatalogStorage.CatalogSnapshot> loadBinarySnapshot()`

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1–T21 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | Existing foundation, location management, and scanner/filter tests | R1–R4 | existing |
| T22 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `statisticsCalculator_computes1DAnd2DPrimitiveArraysAndEnforcesDefensiveCopies` | R1 | new |
| T23 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `statisticsCalculator_handlesEmptyInputsAndRejectsNullArguments` | R1 | new |
| T24 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `reportExporter_writesFormattedUtf8ReportAtomicallyAndCleansUpTempFile` | R2 | new |
| T25 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `reportExporter_throwsStorageOperationExceptionOnWriteFailureAndRejectsNulls` | R2 | new |
| T26 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `binaryCatalogStorage_savesAndLoadsRoundTripSnapshotAndReturnsEmptyWhenMissing` | R3 | new |
| T27 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `binaryCatalogStorage_throwsStorageOperationExceptionOnCorruptedFileOrWriteFailure` | R3 | new |
| T28 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_updatesStatisticsOnLifecycleChangesExportsReportAndSavesBinaryOnExit` | R4 | new |

Commands: `./scripts/build.sh`, `./scripts/test.sh`, `./scripts/lint.sh`.

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| None (removes `TD-location-management-1`) | All persistence and statistics components are completed in this increment | N/A |

## Size

- Production code: about 280 changed lines in 4 files; new runtime dependencies: none.
- Increment budget: fits.

## Risks

- Corrupted or truncated binary files causing unchecked `EOFException` or negative allocation sizes: mitigate by validating array lengths (`>= 0` and matching `SUPPORTED_EXTENSIONS.size()`) and catching `IOException | IllegalArgumentException | InvalidPathException` inside `BinaryCatalogStorage.load()` to throw `StorageOperationException`.
- Default storage paths polluting working directory during existing 1-arg `MultimediaCatalog` tests (`T11`–`T21`): mitigate by resolving default `data/catalog.bin` and `reports/multimedia-report.txt` paths against `locationRepository.getLocationsFile().getParent()`.
