# Multimedia File Manager (Tema 4)

A Java 21 console application for monitoring multiple directories on disk and cataloging, filtering, and reporting on multimedia files (audio `.mp3`, `.wav` and images `.jpg`, `.png`). Monitored directory paths are persisted in `locations.txt`, all working data is managed in memory inside collections of custom domain objects (`List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) and 1D/2D primitive arrays (`int[]`, `long[]`, `long[][]`), and statistics/snapshots are saved to text (`reports/multimedia-report.txt`) and binary (`data/catalog.bin`) files.

Status: `project-foundation` complete (build/lint/test/run toolchain, domain model, and exception hierarchy). The roadmap is in [`docs/ROADMAP.md`](docs/ROADMAP.md) and the design is documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Features

| Feature | Interface | Since |
| --- | --- | --- |
| Cross-platform Java 21 build, lint (`-Xlint:all -Werror` + `javadoc`), test, and run scripts | `./scripts/{build,lint,test,run}.sh` | `project-foundation` |
| Validated immutable domain models (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`) | `ro.ase.multimedia.model` | `project-foundation` |
| Unchecked domain exception hierarchy (`MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) | `ro.ase.multimedia.exception` | `project-foundation` |
| Application entry point and zero-dependency automated unit test suite | `ro.ase.multimedia.Main`, `ro.ase.multimedia.TestRunner` | `project-foundation` |

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
./scripts/build.sh   # compile main and test sources
./scripts/lint.sh    # javac -Xlint:all -Werror and javadoc -Xdoclint:all -Werror check
./scripts/test.sh    # run automated test suite (ro.ase.multimedia.TestRunner)
./scripts/run.sh     # start the console application (ro.ase.multimedia.Main)
```

## Development workflow

Every change is a task that goes through eight phases, each one a skill in `.agents/skills/`; the rules are in [`AGENTS.md`](AGENTS.md). A task starts with `/task-scoping`, which takes the next roadmap item when it gets no request. The later phases (`/task-planning`, `/task-plan-critique`, `/task-development`, `/task-code-review`, `/task-testing`, `/task-gitops`, `/task-doc-updates`) find the task from the current branch. The files of a task are in `work/<slug>/` while it is open and in `work/done/<slug>/` after it is finished.
