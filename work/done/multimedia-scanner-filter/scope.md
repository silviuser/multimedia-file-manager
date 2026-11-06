---
task: multimedia-scanner-filter
artifact: scope
version: 1
status: confirmed
branch: multimedia-scanner-filter
roadmap_item: multimedia-scanner-filter
---

# Scope: Multimedia Directory Scanner and Catalog File Filtering

## Goal

The application can recursively scan accessible monitored directories for supported multimedia files (`.mp3`, `.wav`, `.jpg`, `.png`), extract their metadata (`MultimediaFile`), maintain in-memory media collections (`List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>`) inside `MultimediaCatalog`, and filter or search cataloged files by `MediaType`, file extension, or filename substring.

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | `MultimediaScanner` recursively traverses an `ACCESSIBLE` monitored directory without following symbolic links, identifying regular files with supported extensions (`.mp3`, `.wav`, `.jpg`, `.png` case-insensitively), extracting metadata (`name`, `extension`, `MediaType`, `sizeBytes`, `directoryPath`, `fullPath`), and returning discovered `MultimediaFile` entries sorted deterministically by normalized `fullPath` along with non-fatal warnings for unreadable subdirectories or files (`G3`, `G4`, `S4`, `S6`, `JAVA-5`, `JAVA-7`, `VAL-4`, `VAL-5`). | Given an accessible directory tree containing `.mp3`, `.wav`, `.jpg`, `.png` (in mixed case), unsupported files, and subdirectories, when `MultimediaScanner.scanDirectory(directoryPath)` is called, then only regular files with supported extensions are returned as `MultimediaFile` objects with accurate size and paths, without modifying any file on disk. |
| R2 | `MultimediaCatalog` maintains `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` (`EnumMap`) in memory, automatically scanning accessible monitored directories on `initialize()`, `addDirectory(...)`, `removeDirectory(...)`, and on-demand `rescan()`, deduplicating overlapping directory trees by `fullPath` (first monitored directory in insertion order owns the file) (`G3`, `G4`, `S4`, `S6`, `JAVA-2`, `JAVA-4`, `DATA-2`). | Given monitored directories (including `MISSING`/`INACCESSIBLE` or overlapping parent/child directories), when `initialize()`, `addDirectory(...)`, `removeDirectory(...)`, or `rescan()` runs, then `getMultimediaFiles()` and `getFilesByMediaType()` return unmodifiable views of the current deduplicated files for active accessible directories only. |
| R3 | `MultimediaCatalog` filters in-memory multimedia files by `MediaType` (`AUDIO` or `IMAGE`) (`G3`, `G4`, `S4`, `JAVA-2`, `JAVA-7`, `VAL-1`, `VAL-2`). | Given a populated catalog, when `filterByMediaType(mediaType)` is called, then it returns an unmodifiable list of matching `MultimediaFile` entries for `AUDIO` or `IMAGE`, or throws `IllegalArgumentException` if `mediaType` is `null`. |
| R4 | `MultimediaCatalog` filters in-memory multimedia files by supported extension (`.mp3`, `.wav`, `.jpg`, `.png`, case-insensitively, with or without a leading dot) (`G4`, `S4`, `JAVA-7`, `VAL-1`, `VAL-2`, `VAL-4`). | Given a populated catalog, when `filterByExtension(extension)` is called with a valid extension (e.g. `"MP3"` or `".jpg"`), then it returns an unmodifiable list of matching `MultimediaFile` entries, or throws `IllegalArgumentException` if the extension is `null`, blank, or unsupported. |
| R5 | `MultimediaCatalog` searches in-memory multimedia files by case-insensitive filename substring (`G4`, `S4`, `S5`, `JAVA-7`, `VAL-1`, `VAL-2`). | Given a populated catalog, when `searchByName(query)` is called with a non-blank string, then it returns an unmodifiable list of `MultimediaFile` entries whose `name` contains `query.trim()` case-insensitively, or throws `IllegalArgumentException` if `query` is `null` or blank. |

## Out of scope

- 1D and 2D primitive array calculation (`StatisticsCalculator`: `int[]`, `long[]`, `long[][]`), text report export (`ReportExporter`), and binary snapshot persistence (`BinaryCatalogStorage`) — covered in `statistics-and-reports`.
- Interactive console menu (`ConsoleMenu`) and `Main.main` startup/shutdown wiring — covered in `console-menu-app`.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| `Files.walkFileTree` with `SimpleFileVisitor<Path>` in `MultimediaScanner` | Handles unreadable subdirectories/files gracefully via `visitFileFailed` without aborting the entire directory scan (`S6`); provides `BasicFileAttributes` (`isRegularFile`, `size`) without extra system calls; does not follow symlinks by default | Slightly more verbose than `Files.walk` | chosen |
| `Files.walk(Path)` with `try-with-resources` (`Stream<Path>`) | Concise stream pipeline (`JAVA-6`) | Throws `UncheckedIOException` (`AccessDeniedException`) mid-stream on any unreadable subdirectory, aborting the rest of the scan; requires extra `Files.size(path)` / `Files.isRegularFile(path)` calls | rejected |
| `EnumMap<MediaType, List<MultimediaFile>>` paired with `List<MultimediaFile>` in `MultimediaCatalog` | Satisfies `G3` and `JAVA-4` (`List<MultimediaFile>` + `Map<MediaType, List<MultimediaFile>>`), gives $O(1)$ lookup for `filterByMediaType`, and preserves insertion/sorted order | Both collections must be rebuilt together on scan/update | chosen |
| Single `List<MultimediaFile>` with on-the-fly grouping only | Only one collection field to maintain | Does not maintain a persistent `Map<MediaType, List<MultimediaFile>>` collection in memory as specified in `ARCHITECTURE.md` and `JAVA-4` | rejected |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/SimpleFileVisitor.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumMap.html

## Constraints and assumptions

- `MultimediaScanner` is strictly read-only (`VAL-5`, `S3`) and does not follow symbolic links, avoiding symlink loops.
- `MultimediaCatalog` keeps a default constructor `MultimediaCatalog(LocationRepository)` that instantiates a default `MultimediaScanner()`, plus an overloaded constructor `MultimediaCatalog(LocationRepository, MultimediaScanner)` for explicit dependency injection.
- Unreadable files or subdirectories encountered during traversal are recorded as non-fatal warnings in `ScanResult` (and included in `initialize()` / `rescan()` warnings) rather than crashing (`S6`, `VAL-3`).

## Impact

- Code areas touched: `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaScanner.java`, `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`, `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`.
- Blast radius: low (adds `MultimediaScanner` and extends `MultimediaCatalog` while keeping existing directory management methods and `T1`–`T16` tests backward-compatible).
- Increment budget: fits (2 production files touched/created, ~200 lines of new production code, 0 external dependencies).

## Split and placeholders

- `MultimediaCatalog` integrates `MultimediaScanner`, `List<MultimediaFile>`, and `Map<MediaType, List<MultimediaFile>>` in this increment; 1D/2D primitive array calculation (`StatisticsCalculator`), text report export (`ReportExporter`), and binary persistence (`BinaryCatalogStorage`) remain deferred under `TODO(TD-location-management-1)` until `statistics-and-reports`.

## Follow-ups

- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.

## Open questions

- None
