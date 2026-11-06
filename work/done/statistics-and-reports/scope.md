---
task: statistics-and-reports
artifact: scope
version: 1
status: confirmed
branch: statistics-and-reports
roadmap_item: statistics-and-reports
---

# Scope: Primitive Array Statistics Calculator, Text Report Exporter, and Binary Catalog Storage

## Goal

The application calculates 1D (`int[] countsByExtension`, `long[] bytesByExtension`) and 2D (`long[][] directoryExtensionMatrix`) primitive array statistics from in-memory collections via `StatisticsCalculator`, exports formatted human-readable statistics and inventory reports to `reports/multimedia-report.txt` via `ReportExporter`, and saves/restores binary catalog snapshots (`data/catalog.bin`) via `BinaryCatalogStorage`, all integrated into `MultimediaCatalog`.

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | `StatisticsCalculator` computes an immutable `StatisticsSnapshot` containing 1D primitive arrays (`int[] countsByExtension`, `long[] bytesByExtension` indexed by `MediaType.SUPPORTED_EXTENSIONS` order `.mp3`, `.wav`, `.jpg`, `.png`) and a 2D primitive matrix (`long[][] directoryExtensionMatrix` where cell `[dirIndex][extIndex]` holds the file count for monitored directory `dirIndex` and extension `extIndex`) with defensive deep copies on construction and access (`G3`, `G4`, `JAVA-2`, `JAVA-3`, `JAVA-7`). | Given a list of `MonitoredDirectory` entries and a list of `MultimediaFile` entries, when `StatisticsCalculator.calculate(directories, files)` is called, then the returned `StatisticsSnapshot` exposes defensive copies of `int[] countsByExtension`, `long[] bytesByExtension`, and `long[][] directoryExtensionMatrix` matching the exact per-extension counts, byte totals, and per-directory extension file counts. |
| R2 | `ReportExporter` writes a formatted UTF-8 text report (`multimedia-report.txt`) via a sibling `.tmp` file and atomic replacement, containing summary totals, the 1D per-extension table, the 2D directory-by-extension matrix, and the cataloged multimedia file inventory (`G1`, `S4`, `JAVA-5`, `JAVA-6`, `DATA-1`, `DATA-4`, `DATA-6`, `DATA-7`, `VAL-1`). | Given a populated catalog state and a target report `Path`, when `ReportExporter.export(directories, files, statistics)` is called, then a UTF-8 text file is atomically written containing directory statuses, 1D extension counts and bytes, 2D directory-by-extension matrix rows, and file details, or `StorageOperationException` is thrown on I/O failure. |
| R3 | `BinaryCatalogStorage` writes and reads a full binary snapshot (`catalog.bin`) using `DataOutputStream` and `DataInputStream` with `try-with-resources`, a magic/version header, monitored directories, discovered `MultimediaFile` entries, and the 1D/2D primitive arrays (`int[]`, `long[]`, `long[][]`) via atomic sibling `.tmp` replacement (`G1`, `G2`, `G3`, `G4`, `JAVA-5`, `JAVA-6`, `DATA-1`, `DATA-6`, `DATA-7`, `VAL-1`). | Given monitored directories, multimedia files, and a `StatisticsSnapshot`, when `BinaryCatalogStorage.save(...)` followed by `BinaryCatalogStorage.load()` is called, then `load()` returns an `Optional<CatalogSnapshot>` whose directories, files, and 1D/2D primitive arrays equal the saved state, returns `Optional.empty()` if the binary file does not exist, and throws `StorageOperationException` if the binary file is corrupted or unreadable. |
| R4 | `MultimediaCatalog` integrates `StatisticsCalculator`, `ReportExporter`, and `BinaryCatalogStorage`, automatically recalculating the in-memory `StatisticsSnapshot` on `initialize()`, `addDirectory(...)`, `removeDirectory(...)`, and `rescan()`, exposing `getStatistics()`, `exportReport()`, `saveBinarySnapshot()`, and `loadBinarySnapshot()`, and saving both `locations.txt` and `catalog.bin` in `saveAllOnExit()` (`G1`, `G2`, `G3`, `G4`, `JAVA-2`, `DATA-2`, `DATA-3`). | Given a `MultimediaCatalog` configured with `LocationRepository`, `BinaryCatalogStorage`, and `ReportExporter`, when directories are initialized, added, or removed and `exportReport()` or `saveAllOnExit()` is called, then `getStatistics()` reflects current in-memory 1D/2D arrays and `locations.txt`, `multimedia-report.txt`, and `catalog.bin` are persisted accurately. |

## Out of scope

- Interactive console menu (`ConsoleMenu`) and `Main.main` startup/shutdown CLI wiring — covered in `console-menu-app`.
- Full course documentation export (`docs/PROJECT_DOCUMENTATION.md`) and manual test guide — covered in `javadoc-and-documentation`.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| `DataOutputStream` / `DataInputStream` over `Files.newOutputStream` / `Files.newInputStream` for `BinaryCatalogStorage` | Explicitly required by `ARCHITECTURE.md` and `DATA-6`; compact deterministic binary layout with magic/version validation and no `Serializable` security/versioning overhead | Requires explicit field-by-field read/write ordering | chosen |
| `ObjectOutputStream` / `ObjectInputStream` (`java.io.Serializable`) | Automatic object graph serialization | Violates `DATA-6`, adds Java serialization stream overhead, and requires making immutable `Path`-backed domain models `Serializable` | rejected |
| Defensive cloning of `int[]`, `long[]`, and `long[][]` inside `StatisticsCalculator.StatisticsSnapshot` | Preserves immutability (`JAVA-2`) while strictly using `int[]`, `long[]`, and `long[][]` primitive arrays (`JAVA-3`, `G3`) | Minor array copy allocation on getter calls (4 extensions, small directory count) | chosen |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/DataOutputStream.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/DataInputStream.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html

## Constraints and assumptions

- Supported extensions order in 1D and 2D arrays is fixed by `MediaType.SUPPORTED_EXTENSIONS` (`[0] = ".mp3"`, `[1] = ".wav"`, `[2] = ".jpg"`, `[3] = ".png"`), and directory rows in `long[][] directoryExtensionMatrix` match `MultimediaCatalog.getMonitoredDirectories()` insertion order (`D9`).
- `MultimediaCatalog` preserves existing constructors (`MultimediaCatalog(LocationRepository)` and `MultimediaCatalog(LocationRepository, MultimediaScanner)`) using default paths (`Path.of("data", "catalog.bin")` and `Path.of("reports", "multimedia-report.txt")`) and adds an overloaded constructor accepting `LocationRepository`, `MultimediaScanner`, `StatisticsCalculator`, `BinaryCatalogStorage`, and `ReportExporter` for isolated testing (`DATA-7`, `TEST-2`, `TEST-6`).
- Resolved tech debt: removes `TD-location-management-1` from `MultimediaCatalog`.

## Impact

- Code areas touched:
  - `project_ppoo/src/main/java/ro/ase/multimedia/service/StatisticsCalculator.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/storage/ReportExporter.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/storage/BinaryCatalogStorage.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java` (updated)
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` (updated)
- Blast radius: low (adds three cohesive classes and extends `MultimediaCatalog` while preserving existing signatures and tests `T1`–`T21`).
- Increment budget: fits (4 production files, ~280 lines of new/changed production code, 0 external dependencies).

## Split and placeholders

- None (fits within a single increment and resolves `TD-location-management-1`).

## Follow-ups

- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.
- `javadoc-and-documentation`: complete JavaDoc verification, repeatable manual test scenarios, and course documentation.

## Open questions

- None
