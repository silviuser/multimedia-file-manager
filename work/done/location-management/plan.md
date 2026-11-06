---
task: location-management
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Monitored Location Persistence and Catalog Directory Management

## Summary

Implement `LocationRepository` in `ro.ase.multimedia.storage` to load and atomically save monitored directory paths in `locations.txt` (`R1`, `R2`), and implement `MultimediaCatalog` in `ro.ase.multimedia.service` to manage the in-memory `List<MonitoredDirectory>` and `Set<Path>` collections across startup restoration, status classification (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), validated addition, non-destructive reference removal, and shutdown persistence (`R3`–`R6`). Extend `TestRunner` with isolated temporary-directory tests covering all requirements.

## Context

- `work/location-management/scope.md`: confirms `LocationRepository` atomic `.tmp` saves (`DATA-4`), dual-collection directory tracking (`List<MonitoredDirectory>` + `Set<Path>`, `JAVA-4`), startup warning collection without console I/O (`VAL-3`, `JAVA-5`), and `TD-location-management-1` placeholder for deferred file scanning/reporting.
- `docs/ARCHITECTURE.md`: defines `LocationRepository` and `MultimediaCatalog` responsibilities (`G1`–`G5`, `S1`–`S3`, `S6`, `D2`, `D3`, `D5`).
- `docs/standards/{java,data,validation,testing,git}.md`: enforces `JAVA-1`, `JAVA-2`, `JAVA-4`–`JAVA-9`, `DATA-1`–`DATA-5`, `DATA-7`, `VAL-1`–`VAL-3`, `VAL-5`, and `TEST-1`–`TEST-6`.
- `project_ppoo/src/main/java/ro/ase/multimedia/model/{MonitoredDirectory,DirectoryStatus}.java` and `exception/*.java`: existing domain models and unchecked exceptions to reuse.

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `project_ppoo/src/main/java/ro/ase/multimedia/storage/LocationRepository.java` | create | UTF-8 text persistence for `locations.txt` with missing-file initialization, malformed-line warnings, and atomic sibling `.tmp` writes (`R1`, `R2`, `DATA-1`, `DATA-4`, `DATA-7`, `VAL-3`). |
| 2 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java` | create | In-memory directory catalog (`List<MonitoredDirectory>`, `Set<Path>`) supporting startup load, status classification, add, non-destructive remove, status refresh, and exit save (`R3`–`R6`, `JAVA-2`, `JAVA-4`, `DATA-2`, `DATA-3`, `DATA-5`, `VAL-2`, `VAL-5`). |
| 3 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | modify | Add isolated `Files.createTempDirectory` unit tests (`T9`–`T16`) for `R1`–`R6` while keeping existing tests `T1`–`T8` passing (`TEST-2`, `TEST-3`, `TEST-5`, `TEST-6`). |

## Steps

### Step 1. Implement `LocationRepository` in `ro.ase.multimedia.storage` (`R1`, `R2`)

Create `LocationRepository` accepting `Path locationsFile` in its constructor (rejecting `null` with `IllegalArgumentException` and normalizing via `toAbsolutePath().normalize()`, `DATA-7`, `JAVA-2`). Define a public nested record `LoadResult(List<Path> paths, List<String> warnings)` (compact constructor using `List.copyOf`) with full JavaDoc (`JAVA-9`).
- `load()`: if `locationsFile` does not exist, create parent directories if needed and create an empty file (`Files.createFile`), returning `new LoadResult(List.of(), List.of())` (`VAL-3`, `S6`). Otherwise read lines with `Files.readAllLines(locationsFile, StandardCharsets.UTF_8)`, skip blank lines, parse each trimmed line with `Path.of(trimmed).toAbsolutePath().normalize()`, catch `InvalidPathException` to append a descriptive warning to `warnings` without aborting (`VAL-3`), and wrap any `IOException` in `StorageOperationException` (`VAL-1`).
- `save(List<MonitoredDirectory> directories)`: validate `directories != null`, map each non-null entry to `dir.getPath().toAbsolutePath().normalize().toString()`, ensure parent directory exists, write UTF-8 lines to sibling `<fileName>.tmp`, and atomically replace target (`DATA-4`):

```java
Path tempFile = locationsFile.resolveSibling(locationsFile.getFileName() + ".tmp");
try {
    if (locationsFile.getParent() != null) {
        Files.createDirectories(locationsFile.getParent());
    }
    Files.write(tempFile, lines, StandardCharsets.UTF_8);
    try {
        Files.move(tempFile, locationsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ex) {
        Files.move(tempFile, locationsFile, StandardCopyOption.REPLACE_EXISTING);
    }
} catch (IOException ex) {
    try {
        Files.deleteIfExists(tempFile);
    } catch (IOException suppressed) {
        ex.addSuppressed(suppressed);
    }
    throw new StorageOperationException("Failed to save monitored locations to: " + locationsFile, ex);
}
```

### Step 2. Implement `MultimediaCatalog` in `ro.ase.multimedia.service` (`R3`–`R6`)

Create `MultimediaCatalog` holding `private final LocationRepository locationRepository`, `private final List<MonitoredDirectory> monitoredDirectories = new ArrayList<>()`, and `private final Set<Path> monitoredPaths = new HashSet<>()` (`JAVA-2`, `JAVA-4`, `DATA-2`). Include a `TODO(TD-location-management-1)` comment where `MultimediaScanner`, `StatisticsCalculator`, `BinaryCatalogStorage`, and `ReportExporter` will be integrated in `multimedia-scanner-filter` and `statistics-and-reports`.
- `classifyDirectory(Path path)` (package-private/public static helper): returns `DirectoryStatus.MISSING` if `!Files.exists(path)`, `DirectoryStatus.INACCESSIBLE` if `!Files.isDirectory(path) || !Files.isReadable(path)`, or `DirectoryStatus.ACCESSIBLE` otherwise (`S6`, `VAL-3`), catching `SecurityException` as `INACCESSIBLE`.
- `initialize()`: calls `locationRepository.load()`, clears in-memory collections, iterates loaded paths (skipping duplicates via `monitoredPaths.add(normalized)`), classifies status via `classifyDirectory(normalized)`, adds `new MonitoredDirectory(normalized, status, Instant.now().toString())` to `monitoredDirectories`, and records warnings for `MISSING` or `INACCESSIBLE` directories. Returns `List<String>` of all warnings (`R3`, `VAL-3`, `JAVA-7`).
- `addDirectory(String rawPath)`: validates `rawPath != null && !rawPath.isBlank()`, parses and normalizes `Path` (catching `InvalidPathException` -> `InvalidDirectoryException`), checks `Files.exists`, `Files.isDirectory`, and `Files.isReadable` (throwing `InvalidDirectoryException` with a clear reason if any check fails, `VAL-2`), checks `!monitoredPaths.contains(normalized)` (throwing `DuplicateLocationException` if already present, `VAL-1`), adds to `monitoredDirectories` and `monitoredPaths`, and calls `locationRepository.save(monitoredDirectories)` (`R4`, `DATA-3`).

```java
if (!Files.exists(normalized)) {
    throw new InvalidDirectoryException("Directory does not exist: " + normalized);
}
if (!Files.isDirectory(normalized)) {
    throw new InvalidDirectoryException("Path is not a directory: " + normalized);
}
if (!Files.isReadable(normalized)) {
    throw new InvalidDirectoryException("Directory is not readable: " + normalized);
}
if (monitoredPaths.contains(normalized)) {
    throw new DuplicateLocationException("Directory is already monitored: " + normalized);
}
```

- `removeDirectory(String rawPath)`: validates non-blank and parses normalized `Path` (throwing `InvalidDirectoryException` on null/blank/invalid syntax), finds matching entry in `monitoredDirectories` (throwing `LocationNotFoundException` if not monitored, `VAL-1`), removes it from `monitoredDirectories` and `monitoredPaths`, and calls `locationRepository.save(monitoredDirectories)`. Never calls `Files.delete` on the user path (`R5`, `DATA-5`, `VAL-5`, `S3`).
- `refreshDirectoryStatuses()` and `saveAllOnExit()`: `refreshDirectoryStatuses()` updates each entry in `monitoredDirectories` via `entry.withStatus(classifyDirectory(entry.getPath()))`; `saveAllOnExit()` refreshes statuses and calls `locationRepository.save(monitoredDirectories)` (`R6`, `DATA-3`).
- `getMonitoredDirectories()`: returns `List.copyOf(monitoredDirectories)` (`JAVA-2`).

### Step 3. Add isolated unit tests in `TestRunner` (`R1`–`R6`)

Extend `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` with a recursive temp-directory cleanup helper (`deleteRecursively(Path root)` using `try (Stream<Path> walk = Files.walk(root))` in reverse order, `JAVA-6`, `TEST-2`) and 8 new test methods (`T9`–`T16`) mapped to `R1`–`R6`.

## Interfaces and data

- Package `ro.ase.multimedia.storage`:
  - `LocationRepository(Path locationsFile)`
  - `LocationRepository.LoadResult(List<Path> paths, List<String> warnings)`
  - `load(): LoadResult` (throws `StorageOperationException`)
  - `save(List<MonitoredDirectory> directories): void` (throws `StorageOperationException`)
  - `getLocationsFile(): Path`
- Package `ro.ase.multimedia.service`:
  - `MultimediaCatalog(LocationRepository locationRepository)`
  - `initialize(): List<String>`
  - `addDirectory(String rawPath): MonitoredDirectory` (throws `InvalidDirectoryException`, `DuplicateLocationException`, `StorageOperationException`)
  - `removeDirectory(String rawPath): MonitoredDirectory` (throws `InvalidDirectoryException`, `LocationNotFoundException`, `StorageOperationException`)
  - `refreshDirectoryStatuses(): void`
  - `saveAllOnExit(): void` (throws `StorageOperationException`)
  - `getMonitoredDirectories(): List<MonitoredDirectory>`
  - `classifyDirectory(Path path): DirectoryStatus`
- New runtime dependencies: none (`D1`, `JAVA-1`).

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1–T8 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | Existing foundation tests (`MediaType`, `MonitoredDirectory`, `MultimediaFile`, exceptions, `Main`) continue to pass (`TEST-6`) | — | existing |
| T9 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `locationRepository_loadCreatesMissingFileAndParsesValidLinesWithWarningsForMalformed` (missing file created empty; blank lines ignored; invalid path syntax recorded in warnings) | R1 | new |
| T10 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `locationRepository_saveWritesUtf8LinesAtomicallyAndCleansUpTempFile` (writes normalized paths, leaves no `.tmp` sibling, throws `StorageOperationException` when target is a directory) | R1, R2 | new |
| T11 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_initializeRestoresDeduplicatedDirectoriesAndClassifiesStatusesWithWarnings` (`ACCESSIBLE`, `MISSING`, and file-as-`INACCESSIBLE` statuses plus warnings) | R3 | new |
| T12 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_addDirectoryAddsAccessibleDirectoryAndPersistsToLocationsFile` (updates `getMonitoredDirectories()` and `locations.txt`) | R4 | new |
| T13 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_addDirectoryRejectsInvalidMissingFileOrDuplicatePaths` (blank, missing path, regular file -> `InvalidDirectoryException`; already-added path -> `DuplicateLocationException`) | R4 | new |
| T14 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_removeDirectoryDeletesReferenceOnlyAndKeepsDiskFilesIntact` (removes directory from memory and `locations.txt`, even when `MISSING`, while sample `.mp3` inside directory remains untouched on disk `S3`) | R5 | new |
| T15 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_removeDirectoryRejectsUnmonitoredOrBlankPath` (unmonitored path -> `LocationNotFoundException`, blank -> `InvalidDirectoryException`) | R5 | new |
| T16 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_saveAllOnExitRefreshesStatusesAndFlushesCurrentListToDisk` (deleted monitored directory becomes `MISSING` on refresh and `saveAllOnExit()` writes current list to `locations.txt`) | R6 | new |

Commands: `./scripts/build.sh`, `./scripts/lint.sh`, and `./scripts/test.sh` (all must exit `0` with zero warnings and 16 passing tests).

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| `TODO(TD-location-management-1)` in `MultimediaCatalog` | Media file scanning (`MultimediaScanner`), 1D/2D array recalculation (`StatisticsCalculator`), and binary/report persistence are out of scope for this task | `multimedia-scanner-filter`, `statistics-and-reports` |

## Size

- Production code: about 245 changed lines in 2 new files (`LocationRepository.java`, `MultimediaCatalog.java`); new runtime dependencies: none.
- Increment budget: fits (<= 300 lines and <= 10 production files).

## Risks

- `StandardCopyOption.ATOMIC_MOVE` may throw `AtomicMoveNotSupportedException` on some WSL/Windows mounted filesystems: mitigated in Step 1 by catching `AtomicMoveNotSupportedException` and falling back to `Files.move(tempFile, locationsFile, StandardCopyOption.REPLACE_EXISTING)`.
- Null bytes (`\0`) are the only universally invalid path character on Linux whereas Windows rejects `<>:"|?*`: mitigated in `T9` by including a null-character line (`"invalid\0path"`) so `InvalidPathException` is deterministically triggered on both Linux/WSL and Windows.
