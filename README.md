# Multimedia File Manager (Tema 4)

A Java 21 console application for monitoring multiple directories on disk and cataloging, filtering, and reporting on multimedia files (audio `.mp3`, `.wav` and images `.jpg`, `.png`). Monitored directory paths are persisted in `locations.txt`, all working data is managed in memory inside collections of custom domain objects (`List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) and 1D/2D primitive arrays (`int[]`, `long[]`, `long[][]`), and statistics/snapshots are saved to text (`reports/multimedia-report.txt`) and binary (`data/catalog.bin`) files.

Status: all 6 roadmap increments complete (`project-foundation`, `location-management`, `multimedia-scanner-filter`, `statistics-and-reports`, `console-menu-app`, and `javadoc-and-documentation`: build/lint/test/run/javadoc toolchain, domain model, exception hierarchy, `locations.txt` persistence, in-memory monitored directory catalog, recursive multimedia scanner, catalog file filtering/searching, 1D/2D primitive array statistics calculator, UTF-8 text report exporter, binary catalog snapshot storage, interactive console menu lifecycle, package- and class-level JavaDoc, repeatable manual console verification, and Romanian academic project documentation in [`docs/PROJECT_DOCUMENTATION.md`](docs/PROJECT_DOCUMENTATION.md)). The roadmap is in [`docs/ROADMAP.md`](docs/ROADMAP.md) and the design is documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Features

| Feature | Interface | Since |
| --- | --- | --- |
| Cross-platform Java 21 build, lint (`-Xlint:all -Werror` + `javadoc`), test, JavaDoc HTML generation, and run scripts | `./scripts/{build,lint,test,javadoc,run}.sh` | `project-foundation`, `javadoc-and-documentation` |
| Validated immutable domain models (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`) | `ro.ase.multimedia.model` | `project-foundation` |
| Unchecked domain exception hierarchy (`MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) | `ro.ase.multimedia.exception` | `project-foundation` |
| Application entry point and zero-dependency automated unit test suite | `ro.ase.multimedia.Main`, `ro.ase.multimedia.TestRunner` | `project-foundation` |
| UTF-8 monitored location persistence (`locations.txt`) with missing-file initialization, malformed-line warnings, and atomic sibling `.tmp` writes | `ro.ase.multimedia.storage.LocationRepository` | `location-management` |
| In-memory directory catalog (`List<MonitoredDirectory>`, `Set<Path>`), startup status classification (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), validated addition, non-destructive reference removal, and exit persistence | `ro.ase.multimedia.service.MultimediaCatalog` | `location-management` |
| Recursive multimedia directory scanner (`Files.walkFileTree`) for `.mp3`, `.wav`, `.jpg`, `.png` with non-fatal traversal warnings and no symlink following | `ro.ase.multimedia.service.MultimediaScanner` | `multimedia-scanner-filter` |
| In-memory media collections (`List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`), automatic rescanning with overlapping directory deduplication, and filtering/searching by `MediaType`, extension, or filename substring | `ro.ase.multimedia.service.MultimediaCatalog` | `multimedia-scanner-filter` |
| 1D (`int[] countsByExtension`, `long[] bytesByExtension`) and 2D (`long[][] directoryExtensionMatrix`) primitive array statistics calculation with defensive deep copies | `ro.ase.multimedia.service.StatisticsCalculator` | `statistics-and-reports` |
| Atomic UTF-8 text report export (`reports/multimedia-report.txt`) and binary catalog snapshot save/restore (`data/catalog.bin`) integrated into catalog lifecycle and exit persistence | `ro.ase.multimedia.storage.ReportExporter`, `ro.ase.multimedia.storage.BinaryCatalogStorage`, `ro.ase.multimedia.service.MultimediaCatalog` | `statistics-and-reports` |
| Interactive console menu (`1`–`6` and `0`/EOF exit) with injectable UTF-8 `InputStream`/`PrintStream`, 1-based index or path directory removal, `MediaType`/extension/keyword sub-menu filtering, 1D/2D statistics display with report/binary export, and full `Main` lifecycle | `ro.ase.multimedia.ui.ConsoleMenu`, `ro.ase.multimedia.Main` | `console-menu-app` |
| Package-level JavaDoc (`package-info.java` across all 6 packages), HTML JavaDoc generator (`docs/javadoc/`), repeatable manual verification walkthrough, and Romanian course documentation (`Tema 4`) ready for Microsoft Word (`.docx`) export | `ro.ase.multimedia/**/package-info.java`, `./scripts/javadoc.sh`, `docs/PROJECT_DOCUMENTATION.md` | `javadoc-and-documentation` |

## Requirements

- Java 21 (JDK 21 or newer with `javac`, `java`, and `javadoc`)
- Git, and GitHub CLI (`gh`) for pull requests
- A coding agent that reads skills, for example Antigravity CLI (`agy`)

## Configuration

| Setting / File | Meaning | Default |
| --- | --- | --- |
| `locations.txt` | Text file in the application folder storing one monitored directory path per line | `./locations.txt` |
| `data/catalog.bin` | Binary file storing serialized catalog metadata and primitive array statistics | `./data/catalog.bin` |
| `reports/multimedia-report.txt` | Formatted text report of multimedia file statistics and inventory | `./reports/multimedia-report.txt` |

## Build, Run and Test

Sources reside in `project_ppoo/src/main/java` and `project_ppoo/src/test/java` and compile into `project_ppoo/out/{main,test}`:

```bash
./scripts/build.sh    # compile main and test sources
./scripts/lint.sh     # javac -Xlint:all -Werror and javadoc -Xdoclint:all -Werror check
./scripts/test.sh     # run automated test suite (ro.ase.multimedia.TestRunner)
./scripts/javadoc.sh  # generate HTML JavaDoc into docs/javadoc/index.html
./scripts/run.sh      # start the console application (ro.ase.multimedia.Main)
```

## Manual Console Verification

You can run a complete, repeatable end-to-end console walkthrough (`TEST-7`, `G6`, `G7`) that exercises every menu option (`1`–`6` and `0`) and inspects all persistence files (`locations.txt`, `reports/multimedia-report.txt`, and `data/catalog.bin`):

```bash
# 1. Create temporary sample directories and multimedia files (.mp3, .wav, .jpg, .png)
rm -rf /tmp/mfm-demo
mkdir -p /tmp/mfm-demo/music /tmp/mfm-demo/photos
printf 'sample-mp3-audio-content' > /tmp/mfm-demo/music/summer_song.mp3
printf 'sample-wav-audio' > /tmp/mfm-demo/music/alert.wav
printf 'sample-jpg-image-content-bytes' > /tmp/mfm-demo/photos/vacation.jpg
printf 'sample-png-image' > /tmp/mfm-demo/photos/cover_song.png

# 2. Launch the interactive application and exercise options 1, 2, 3, 4, 5, 6, and 0:
#    - 1: View monitored directories (initially empty)
#    - 2: Add /tmp/mfm-demo/music and /tmp/mfm-demo/photos
#    - 1: View indexed monitored directories with status ACCESSIBLE and file counts
#    - 4: Display all discovered multimedia files
#    - 5 -> 1 -> AUDIO: Filter by MediaType (AUDIO / IMAGE)
#    - 5 -> 2 -> .png: Filter by file extension (.mp3, .wav, .jpg, .png)
#    - 5 -> 3 -> song: Search by filename keyword
#    - 6: View 1D/2D primitive array statistics and export report & binary snapshot
#    - 3 -> 1: Remove directory #1 (/tmp/mfm-demo/music) reference while keeping disk files intact
#    - 0: Save state to locations.txt & data/catalog.bin and exit
./scripts/run.sh << 'EOF'
1
2
/tmp/mfm-demo/music
2
/tmp/mfm-demo/photos
1
4
5
1
AUDIO
5
2
.png
5
3
song
6
3
1
1
0
EOF

# 3. Inspect persisted files and verify removed directory files remain intact on disk (S3)
cat project_ppoo/locations.txt
cat project_ppoo/reports/multimedia-report.txt
ls -lh project_ppoo/data/catalog.bin
ls -la /tmp/mfm-demo/music

# 4. Clean up temporary demo files and runtime persistence artifacts
rm -rf /tmp/mfm-demo project_ppoo/locations.txt project_ppoo/data project_ppoo/reports
```

## Development workflow

Every change is a task that goes through eight phases, each one a skill in `.agents/skills/`; the rules are in [`AGENTS.md`](AGENTS.md). A task starts with `/task-scoping`, which takes the next roadmap item when it gets no request. The later phases (`/task-planning`, `/task-plan-critique`, `/task-development`, `/task-code-review`, `/task-testing`, `/task-gitops`, `/task-doc-updates`) find the task from the current branch. The files of a task are in `work/<slug>/` while it is open and in `work/done/<slug>/` after it is finished.
