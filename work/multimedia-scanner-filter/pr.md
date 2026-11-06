---
task: multimedia-scanner-filter
artifact: pr
branch: multimedia-scanner-filter
base: main
updated_commit: none
pr: https://github.com/silviuser/multimedia-file-manager/pull/3
---

# feat(service): add multimedia directory scanner and catalog file filtering

## Summary

Adds `MultimediaScanner` to recursively discover regular `.mp3`, `.wav`, `.jpg`, and `.png` files in accessible monitored directories without following symbolic links while capturing non-fatal traversal warnings. Extends `MultimediaCatalog` to maintain `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` (`EnumMap`) in memory, automatically rescanning and deduplicating across overlapping monitored directories on lifecycle events, and exposes filtering/searching by `MediaType`, file extension, and filename substring.

## Changes

- Created `MultimediaScanner` (`project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaScanner.java`) using `Files.walkFileTree` and `SimpleFileVisitor<Path>` to recursively discover regular multimedia files sorted by normalized `fullPath` and return a `ScanResult(List<MultimediaFile> files, List<String> warnings)`.
- Extended `MultimediaCatalog` (`project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`) to maintain `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` (`EnumMap`), trigger `rescan()` on `initialize()`, `addDirectory(...)`, and `removeDirectory(...)` with overlapping directory deduplication, and expose `filterByMediaType(MediaType)`, `filterByExtension(String)`, and `searchByName(String)`.
- Added unit tests `T17`–`T21` (`project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`) covering recursive directory scanning, catalog deduplication and lifecycle rescanning, media type filtering, extension filtering, and filename substring search.

## How to test

```bash
./scripts/build.sh
./scripts/lint.sh
./scripts/test.sh
```

- E1: Scan a nested directory tree containing `.MP3`, `.Jpg`, `.wav`, `.PNG`, `.txt`, and a directory symlink via `MultimediaScanner.scanDirectory` and verify only the 4 supported regular files are returned in deterministic order without following the symlink or modifying disk files.
- E2: Call `MultimediaScanner.scanDirectory` on a missing directory path (returns 0 files and 1 warning) and on `null` (throws `IllegalArgumentException`).
- E3: Initialize `MultimediaCatalog` with an accessible directory and a missing directory, add an overlapping child directory, and remove the parent directory; verify deduplication and read-only guarantees on disk files.
- E4–E5: Exercise `filterByMediaType`, `filterByExtension`, and `searchByName` with valid inputs, mixed-case/whitespace inputs, and invalid (`null`/blank/unsupported) inputs, and verify returned collections are unmodifiable.
- E6: Run `./scripts/run.sh` and verify clean console startup.

## Quality gates

| Gate | Task file | Result |
| --- | --- | --- |
| Plan critique | `critique.md`, round 1 | approved |
| Code review | `review.md`, round 1 | approved |
| Tests | `test-report.md`, round 1 | pass |

## Tech debt

- None (existing `TD-location-management-1` in `MultimediaCatalog.java` remains tracked for `statistics-and-reports`)

## Follow-ups

- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle.
