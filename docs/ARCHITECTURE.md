# Architecture

## Overview

A console-based Java 21 application (`ro.ase.multimedia`) for monitoring directories on disk and cataloging, filtering, and reporting on multimedia files (`.mp3`, `.wav`, `.jpg`, `.png`). All active data is managed in memory inside collections of domain objects (`List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) and primitive 1D/2D arrays (`int[]`, `long[]`, `long[][]`) for statistics. Persistence uses a plain-text configuration file (`locations.txt`), formatted text reports, and binary state snapshots.

## Requirements Traceability (`general_req.txt` & `specif_req.txt`)

| Req ID | Requirement Summary | Architectural Mechanism |
| --- | --- | --- |
| G1 | Text and binary files for data save/restore and statistics/reports | `LocationRepository` (`locations.txt`), `BinaryCatalogStorage` (`catalog.bin`), `ReportExporter` (`multimedia-report.txt`) |
| G2 | Save data on application exit and restore on startup, updating input file | Startup loader in `MultimediaApp` restores `locations.txt` (and binary snapshot); exit handler persists in-memory state back to `locations.txt` and `catalog.bin` |
| G3 | At least two 1D/2D primitive arrays and two different collections of custom objects | Primitive arrays: `int[] countsByExtension`, `long[] bytesByExtension` (1D), `long[][] directoryExtensionMatrix` (2D). Object collections: `Set<MonitoredDirectory>` / `List<MonitoredDirectory>` and `List<MultimediaFile>` / `Map<MediaType, List<MultimediaFile>>` |
| G4 | All modifications performed in memory in arrays/collections, saved on exit | `MultimediaCatalog` mutates in-memory collections and recalculates primitive arrays first; `LocationRepository` syncs `locations.txt` on directory list change (Spec 1/2) and full state on exit (G2/G4) |
| G5 | Validation of console and file data using standard and custom exceptions | Custom exceptions (`InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) + standard exceptions (`IllegalArgumentException`, `IOException`, `NumberFormatException`) |
| G6 | Logical functional flow from startup and data loading to application exit | `Main` -> load `locations.txt` -> validate & scan directories -> compute primitive arrays -> interactive menu loop -> save state & exit |
| G7 | Clear interactive menu for invoking application features | `ConsoleMenu` with numbered options (1–6 and 0 for Exit) reading from injectable `InputStream` and printing to `PrintStream` |
| G8 | Fully functional project with zero compilation or runtime errors | `javac -Xlint:all -Werror` strict compilation, automated JUnit/assert test suite, defensive I/O error handling |
| G9 | Documentation (Word guide + JavaDoc) per course/seminar guidelines | Full JavaDoc on packages, classes, and methods (`javadoc`), plus structured technical documentation in `docs/` |
| S1, S2 | View, add, remove monitored directories stored in a text file in app folder | `LocationRepository` + `MultimediaCatalog` managing `locations.txt` |
| S3 | Removing a location deletes only the reference, never the directory or files on disk | `MultimediaCatalog.removeDirectory()` only removes the entry from memory and `locations.txt`; zero `Files.delete` calls on user media directories |
| S4 | Identify, display, and filter multimedia files (`.mp3`, `.wav`, `.jpg`, `.png`) with name, extension, type, size, location | `MediaType`, `MultimediaFile`, `MultimediaScanner`, and filter methods in `MultimediaCatalog` |
| S5 | Interactive menu for directory CRUD, file listing, filtering/searching, statistics, and exit | `ConsoleMenu` |
| S6 | Error handling for invalid user paths, missing/unwritable `locations.txt`, and missing/inaccessible monitored directories | `DirectoryStatus` (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), validation guards, and non-fatal scan warnings |

## Components

| Component | Location | Responsibility |
| --- | --- | --- |
| `MediaType` | `src/main/java/ro/ase/multimedia/model/MediaType.java` | Enum (`AUDIO`, `IMAGE`) mapping supported extensions (`.mp3`, `.wav`, `.jpg`, `.png`) |
| `DirectoryStatus` | `src/main/java/ro/ase/multimedia/model/DirectoryStatus.java` | Enum (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) representing the disk state of a monitored location |
| `MonitoredDirectory` | `src/main/java/ro/ase/multimedia/model/MonitoredDirectory.java` | Domain class for a monitored directory path, status, and last-scanned metadata |
| `MultimediaFile` | `src/main/java/ro/ase/multimedia/model/MultimediaFile.java` | Domain class for a discovered media file (name, extension, `MediaType`, size in bytes, parent directory path) |
| Custom Exceptions | `src/main/java/ro/ase/multimedia/exception/*.java` | `MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException` |
| `LocationRepository` | `src/main/java/ro/ase/multimedia/storage/LocationRepository.java` | Reads and atomically writes monitored directory paths in `locations.txt` |
| `BinaryCatalogStorage` | `src/main/java/ro/ase/multimedia/storage/BinaryCatalogStorage.java` | Saves and restores binary catalog/statistics snapshots (`catalog.bin`) using `DataOutputStream` / `DataInputStream` |
| `ReportExporter` | `src/main/java/ro/ase/multimedia/storage/ReportExporter.java` | Writes formatted text statistics and inventory reports (`multimedia-report.txt`) |
| `MultimediaScanner` | `src/main/java/ro/ase/multimedia/service/MultimediaScanner.java` | Walks accessible monitored directories and builds `MultimediaFile` instances for supported extensions |
| `StatisticsCalculator` | `src/main/java/ro/ase/multimedia/service/StatisticsCalculator.java` | Populates 1D (`int[]`, `long[]`) and 2D (`long[][]`) primitive arrays from the in-memory collections |
| `MultimediaCatalog` | `src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java` | Coordinates in-memory collections (`List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`), primitive arrays, filtering, and persistence |
| `ConsoleMenu` | `src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java` | Interactive CLI menu loop, input parsing, tabular display, and error presentation |
| `Main` | `src/main/java/ro/ase/multimedia/Main.java` | Application entry point: wires dependencies, loads initial state, starts menu, saves on exit |

## Data model

| Entity / Structure | Type & Fields | Stored in |
| --- | --- | --- |
| `MonitoredDirectory` | `path: Path`, `status: DirectoryStatus`, `addedAt: String` | In-memory `List<MonitoredDirectory>` & `Set<Path>`; persisted in `locations.txt` (text) |
| `MultimediaFile` | `name: String`, `extension: String`, `mediaType: MediaType`, `sizeBytes: long`, `directoryPath: Path`, `fullPath: Path` | In-memory `List<MultimediaFile>` & `Map<MediaType, List<MultimediaFile>>`; snapshot in `catalog.bin` (binary) |
| `countsByExtension` | `int[]` (1D primitive array indexed by supported extension ordinal: `.mp3`, `.wav`, `.jpg`, `.png`) | In-memory inside `StatisticsSnapshot`; exported to text report and binary file |
| `bytesByExtension` | `long[]` (1D primitive array of total file sizes in bytes per extension) | In-memory inside `StatisticsSnapshot`; exported to text report and binary file |
| `directoryExtensionMatrix` | `long[][]` (2D primitive matrix `[directoryIndex][extensionIndex]` holding file counts/bytes per directory and extension) | In-memory inside `StatisticsSnapshot`; exported to text report |

## Main flow

1. **Startup**: `Main` initializes `LocationRepository("locations.txt")`, `BinaryCatalogStorage("catalog.bin")`, `ReportExporter("multimedia-report.txt")`, `MultimediaScanner`, and `MultimediaCatalog`.
2. **Restore & Scan**: `MultimediaCatalog.initialize()` loads paths from `locations.txt` (creating an empty file if missing), validates each directory's accessibility on disk (`ACCESSIBLE` vs `MISSING`/`INACCESSIBLE`), scans accessible directories into `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>`, and computes the 1D/2D primitive arrays (`int[]`, `long[]`, `long[][]`).
3. **Interactive Loop**: `ConsoleMenu.run()` displays options:
   - `1` View monitored directories (with status and file counts)
   - `2` Add a directory (validates existence & directory type -> adds to in-memory collection -> updates `locations.txt` -> rescans & updates primitive arrays)
   - `3` Remove a directory (removes reference from in-memory collection and `locations.txt` without touching disk files -> updates in-memory files & primitive arrays)
   - `4` Display all multimedia files (name, extension, type, size, location)
   - `5` Filter / search multimedia files (by `MediaType`, by extension `.mp3`/`.wav`/`.jpg`/`.png`, or by name substring)
   - `6` View statistics (from 1D/2D primitive arrays) and export text report (`multimedia-report.txt`) & binary snapshot (`catalog.bin`)
   - `0` Exit application
4. **Shutdown / Exit**: `MultimediaCatalog.saveAllOnExit()` flushes the current in-memory directory list to `locations.txt` and writes the binary state snapshot (`catalog.bin`) and summary report.

## Decisions

| ID | Decision | Reason | Task |
| --- | --- | --- | --- |
| D1 | Java 21 with zero external runtime dependencies | Standard `java.nio.file`, `java.io`, and `java.util` cover all file I/O, collections, and console requirements cleanly (`JAVA-1`, `G8`) | before the first task |
| D2 | Keep all working state in memory (`List`/`Set`/`Map` + `int[]`/`long[]`/`long[][]`) and sync `locations.txt` both on directory change and on application exit | Satisfies both `general_req.txt` (G2, G4: in-memory mutations saved on exit) and `specif_req.txt` (S1, S2: auto-update `locations.txt` on add/remove) | before the first task |
| D3 | Strictly read-only access to monitored media directories | Guarantees that removing a location or running the app never deletes or alters user files on disk (`VAL-5`, `S3`) | before the first task |
| D4 | Use both text (`locations.txt`, `multimedia-report.txt`) and binary (`catalog.bin`) files | Directly fulfills `G1` (text and binary files for data save/restore and statistics/reports) | before the first task |
| D5 | Inject `InputStream` and `PrintStream` into `ConsoleMenu` and file `Path`s into repositories | Allows fast, deterministic automated tests using temporary directories and in-memory streams (`TEST-2`, `TEST-4`) | before the first task |
