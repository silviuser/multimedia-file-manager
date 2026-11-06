# Multimedia File Manager (Tema 4)

A Java 21 console application for monitoring multiple directories on disk and cataloging, filtering, and reporting on multimedia files (audio `.mp3`, `.wav` and images `.jpg`, `.png`). Monitored directory paths are persisted in `locations.txt`, all working data is managed in memory inside collections of custom domain objects (`List<MonitoredDirectory>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`) and 1D/2D primitive arrays (`int[]`, `long[]`, `long[][]`), and statistics/snapshots are saved to text (`reports/multimedia-report.txt`) and binary (`data/catalog.bin`) files.

Status: no code yet. The planned increments are listed in [`docs/ROADMAP.md`](docs/ROADMAP.md) and the design is documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Features

| Feature | Interface | Since |
| --- | --- | --- |
| None yet | | |

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

Created by the first roadmap increment (`project-foundation`):

```bash
./scripts/build.sh   # compile main and test sources
./scripts/lint.sh    # javac -Xlint:all -Werror and javadoc check
./scripts/test.sh    # run automated test suite
./scripts/run.sh     # start the interactive console menu
```

## Development workflow

Every change is a task that goes through eight phases, each one a skill in `.agents/skills/`; the rules are in [`AGENTS.md`](AGENTS.md). A task starts with `/task-scoping`, which takes the next roadmap item when it gets no request. The later phases (`/task-planning`, `/task-plan-critique`, `/task-development`, `/task-code-review`, `/task-testing`, `/task-gitops`, `/task-doc-updates`) find the task from the current branch. The files of a task are in `work/<slug>/` while it is open and in `work/done/<slug>/` after it is finished.
