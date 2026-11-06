# Data and persistence standards

Scope: in-memory state management and file I/O (`locations.txt`, binary snapshots, text reports). Rule IDs (`DATA-<n>`) are cited in plans, critiques and reviews.

## DATA-1. All file I/O goes through the `storage` package

Services and UI classes never read or write persistence files directly. They call `LocationRepository` for the monitored locations text file, `BinaryCatalogStorage` for binary state/statistics snapshots, and `ReportExporter` for formatted text reports (`G1`).

## DATA-2. In-memory collections and arrays are authoritative (`G4`)

During program execution, all additions, removals, scans and recalculations happen in memory inside `MultimediaCatalog` (updating `List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`, and the 1D/2D primitive arrays `int[]`, `long[]`, `long[][]`).

## DATA-3. Dual-sync persistence for monitored locations (`G2`, `G4`, `S1`, `S2`)

To satisfy both `specif_req.txt` (`S1`, `S2`: adding or removing a directory automatically updates the text file) and `general_req.txt` (`G2`, `G4`: restoring data at startup and saving in-memory changes to the input file on application exit):
- At startup, `MultimediaCatalog` restores the list of monitored directories from `locations.txt` (creating an empty file if it does not exist yet);
- Adding or removing a directory updates the in-memory collection first and immediately synchronizes `locations.txt`;
- At application exit, `MultimediaCatalog.saveAllOnExit()` writes the final in-memory directory list to `locations.txt` and saves the binary catalog snapshot (`catalog.bin`).

## DATA-4. Safe atomic writes and UTF-8 encoding

Text files (`locations.txt`, `multimedia-report.txt`) use explicit `StandardCharsets.UTF_8`. Updating `locations.txt` writes to a sibling temporary file first and moves it over the target (`Files.move` with `REPLACE_EXISTING`), preventing file corruption on interrupted writes.

```java
Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
Files.write(tempFile, lines, StandardCharsets.UTF_8);
Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
```

## DATA-5. Non-destructive location removal (`S3`)

Removing a monitored location from the application **only** deletes its reference from the in-memory collections and from `locations.txt`. The directory on disk and the multimedia files inside it are **never** deleted, moved or modified (`S3`).

## DATA-6. Both text and binary file persistence (`G1`)

- **Text persistence**: `locations.txt` stores one normalized directory path per line; `multimedia-report.txt` stores human-readable statistics tables generated from the 1D/2D primitive arrays and file collections.
- **Binary persistence**: `BinaryCatalogStorage` writes and reads catalog metadata and primitive array statistics using `DataOutputStream` / `DataInputStream` (`catalog.bin`).

## DATA-7. Configurable file paths for test isolation

`LocationRepository`, `BinaryCatalogStorage` and `ReportExporter` accept their target `Path` via constructor injection. Production `Main` uses default paths in the application folder (`locations.txt`, `data/catalog.bin`, `reports/multimedia-report.txt`), while automated tests pass temporary paths inside `Files.createTempDirectory(...)`.
