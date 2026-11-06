# Roadmap

Each item is one increment, developed as a task (see `AGENTS.md`). Status: planned or done.

| # | Slug | Increment | Status | Pull request |
| --- | --- | --- | --- | --- |
| 1 | project-foundation | Java 21 project structure and build/test/lint scripts, domain model (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`), and custom + standard exception hierarchy (`MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) [G5, G8, S4] | done | https://github.com/silviuser/multimedia-file-manager/pull/1 |
| 2 | location-management | `LocationRepository` (`locations.txt` load on startup, atomic update on change and on exit) and directory management in `MultimediaCatalog` (`List<MonitoredDirectory>`, `Set<Path>`, path validation, non-destructive reference removal, missing/inaccessible directory handling) [G1, G2, G3, G4, G5, S1, S2, S3, S6] | done | https://github.com/silviuser/multimedia-file-manager/pull/2 |
| 3 | multimedia-scanner-filter | `MultimediaScanner` and catalog media management (`List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`): recursive/directory scan for `.mp3`, `.wav`, `.jpg`, `.png`, metadata extraction (name, extension, type, size, location), and filtering/searching by type, extension, or name [G3, G4, S4, S6] | done | https://github.com/silviuser/multimedia-file-manager/pull/3 |
| 4 | statistics-and-reports | `StatisticsCalculator` with 1D (`int[] countsByExtension`, `long[] bytesByExtension`) and 2D (`long[][] directoryExtensionMatrix`) primitive arrays, `ReportExporter` (text report `multimedia-report.txt`), and `BinaryCatalogStorage` (binary save/restore `catalog.bin`) [G1, G2, G3, G4] | done | https://github.com/silviuser/multimedia-file-manager/pull/4 |
| 5 | console-menu-app | Interactive `ConsoleMenu` and `Main` lifecycle: startup restoration & scan, numbered menu options (view/add/remove locations, list/filter files, generate statistics/reports, exit with save), console input validation, and end-to-end CLI tests [G2, G4, G5, G6, G7, G8, S1–S6] | planned | |
| 6 | javadoc-and-documentation | Complete JavaDoc documentation (`javadoc` generation check), repeatable manual test scenarios in `README.md`, and complete project documentation (`docs/PROJECT_DOCUMENTATION.md`) ready for Microsoft Word export per course/seminar guidelines [G8, G9] | planned | |

## Follow-ups

Items found while working on other tasks, not yet scheduled.

| Slug | Description | Found in |
| --- | --- | --- |
| None | | |
