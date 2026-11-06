---
task: location-management
artifact: scope
version: 1
status: confirmed
branch: location-management
roadmap_item: location-management
---

# Scope: Monitored Location Persistence and Catalog Directory Management

## Goal

The application can persist monitored directory paths in `locations.txt` via `LocationRepository` and manage the in-memory directory catalog (`List<MonitoredDirectory>` and `Set<Path>`) via `MultimediaCatalog`, including startup initialization, path validation, status classification (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), and non-destructive reference removal.

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | `LocationRepository` loads normalized paths from a configurable `locations.txt` (`UTF-8`), creating an empty file if missing, ignoring blank lines, and reporting invalid path lines without aborting (`G1`, `G2`, `G5`, `S2`, `S6`, `DATA-1`, `DATA-3`, `DATA-7`, `VAL-3`). | Given a missing or populated `locations.txt` (including blank or malformed lines), when `LocationRepository.load()` is called, then the file is created if absent, valid paths are returned, malformed lines are collected as warnings, and unrecoverable I/O errors throw `StorageOperationException`. |
| R2 | `LocationRepository` saves monitored directory paths to `locations.txt` in `UTF-8` using a sibling temporary file (`<name>.tmp`) and atomic/replace move (`G1`, `G2`, `S1`, `S2`, `DATA-4`, `DATA-6`, `VAL-1`). | Given a list of `MonitoredDirectory` entries, when `LocationRepository.save(...)` is called, then `locations.txt` contains one normalized path string per line in `UTF-8`, no `.tmp` file remains on disk, and write failures throw `StorageOperationException`. |
| R3 | `MultimediaCatalog.initialize()` restores directories from `LocationRepository` into `List<MonitoredDirectory>` and `Set<Path>`, deduplicating paths and classifying each directory on disk as `ACCESSIBLE`, `MISSING`, or `INACCESSIBLE` (`G2`, `G3`, `G4`, `S1`, `S2`, `S6`, `JAVA-2`, `JAVA-4`, `DATA-2`, `VAL-3`). | Given existing and non-existent directory paths in `locations.txt`, when `MultimediaCatalog.initialize()` runs, then `getMonitoredDirectories()` returns an unmodifiable list with accurate `DirectoryStatus` values and startup warnings for missing, inaccessible, or malformed entries. |
| R4 | `MultimediaCatalog.addDirectory(...)` validates that a candidate path is non-blank, exists, is a readable directory, and is not already monitored, then updates in-memory collections (`List<MonitoredDirectory>`, `Set<Path>`) and immediately syncs `locations.txt` (`G3`, `G4`, `G5`, `S1`, `S2`, `S6`, `DATA-2`, `DATA-3`, `VAL-1`, `VAL-2`). | Given a candidate path string, when `addDirectory` is called, then a valid new directory is added as `ACCESSIBLE` in memory and persisted to `locations.txt`, whereas invalid/non-existent/file paths throw `InvalidDirectoryException` and existing paths throw `DuplicateLocationException`. |
| R5 | `MultimediaCatalog.removeDirectory(...)` removes a monitored directory reference from in-memory collections and `locations.txt` (even if its disk status is `MISSING`), without deleting or modifying the directory or any files on disk (`G4`, `G5`, `S1`, `S2`, `S3`, `DATA-3`, `DATA-5`, `VAL-1`, `VAL-5`). | Given a monitored directory containing multimedia files on disk, when `removeDirectory` is called with its path, then the entry is removed from `getMonitoredDirectories()` and `locations.txt`, all disk files remain intact, and removing an unmonitored path throws `LocationNotFoundException`. |
| R6 | `MultimediaCatalog.saveAllOnExit()` flushes the authoritative in-memory directory list to `locations.txt` on shutdown and refreshes directory statuses on demand (`G2`, `G4`, `DATA-2`, `DATA-3`). | Given in-memory monitored directories, when `saveAllOnExit()` is invoked, then `locations.txt` reflects the exact current in-memory directory list. |

## Out of scope

- Recursive directory scanning for `.mp3`, `.wav`, `.jpg`, `.png` files and filtering (`MultimediaScanner`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) — covered in `multimedia-scanner-filter`.
- 1D and 2D primitive array calculation (`StatisticsCalculator`), text report export (`ReportExporter`), and binary snapshot persistence (`BinaryCatalogStorage`) — covered in `statistics-and-reports`.
- Interactive console UI (`ConsoleMenu`) and wiring in `Main.main` — covered in `console-menu-app`.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| Write to sibling `.tmp` file (`resolveSibling`) + `Files.move(..., REPLACE_EXISTING)` (`java.nio.file.Files`) | Prevents partial/corrupted `locations.txt` on interrupted writes; works consistently across Linux, WSL (`drvfs`), and Windows NTFS (`DATA-4`) | Requires temporary file cleanup on unexpected write failure | chosen |
| Direct overwrite with `Files.write(target, lines, TRUNCATE_EXISTING)` | Slightly fewer lines of code | Truncates existing `locations.txt` before write completes, risking data loss on I/O failure; violates `DATA-4` | rejected |
| Dual collection index (`List<MonitoredDirectory>` + `Set<Path>`) in `MultimediaCatalog` | Preserves deterministic insertion order for display/2D matrix indexing while giving $O(1)$ duplicate lookups (`JAVA-4`, `G3`) | Must keep both collections synchronized on add/remove/initialize | chosen |
| Single `LinkedHashSet<MonitoredDirectory>` only | Single collection field | Less convenient for positional indexing required by 2D array `[dirIndex][extIndex]` in `statistics-and-reports` | rejected |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/StandardCopyOption.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/InvalidPathException.html

## Constraints and assumptions

- Plain-text persistence uses `StandardCharsets.UTF_8` with one normalized absolute path string per line (`DATA-4`, `DATA-6`).
- Neither `LocationRepository` nor `MultimediaCatalog` writes to `System.out` or `System.err`; startup warnings (`VAL-3`) are returned as data so `ConsoleMenu` can display them at the UI boundary (`JAVA-5`, `JAVA-7`).
- Removing a location never calls `Files.delete` or `Files.deleteIfExists` on any user directory (`DATA-5`, `VAL-5`, `S3`).

## Impact

- Code areas touched: `project_ppoo/src/main/java/ro/ase/multimedia/storage/LocationRepository.java`, `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`, `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`.
- Blast radius: low (adds two new classes in `storage` and `service` packages and extends `TestRunner`; existing domain/exception classes remain unchanged).
- Increment budget: fits (2 new production files, ~230 lines of production code, 0 external dependencies).

## Split and placeholders

- `MultimediaCatalog` manages `List<MonitoredDirectory>` and `Set<Path>` in this increment; media file scanning (`MultimediaScanner`) and binary/report persistence (`BinaryCatalogStorage`, `ReportExporter`) are marked with `TODO(TD-location-management-1)` placeholders until `multimedia-scanner-filter` and `statistics-and-reports`.

## Follow-ups

- `multimedia-scanner-filter`: `MultimediaScanner` and catalog media file collections/filtering.
- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.

## Open questions

- None
