---
task: statistics-and-reports
artifact: test-report
round: 1
tested_commit: 2fcc372
verdict: pass
---

# Test report: statistics-and-reports, round 1

## Environment

- OpenJDK 21.0.12.1 (`21.0.12.1+1-1-24.04.4-Ubuntu`), Linux 6.18.54.1-microsoft-standard-WSL2 x86_64, local file system (`java.nio.file` temporary directories, no external services)

## Automated tests

- Clean install and test command (`./scripts/build.sh && ./scripts/test.sh && ./scripts/lint.sh`): 28 passed, 0 failed, 0 skipped

| Plan test | Exists | Result |
| --- | --- | --- |
| T1–T21 | yes | pass |
| T22 | yes | pass |
| T23 | yes | pass |
| T24 | yes | pass |
| T25 | yes | pass |
| T26 | yes | pass |
| T27 | yes | pass |
| T28 | yes | pass |

## Acceptance criteria

| Requirement | Verified by | Result |
| --- | --- | --- |
| R1 | T22, T23, check E2 | pass |
| R2 | T24, T25, check E3 | pass |
| R3 | T26, T27, check E4 | pass |
| R4 | T1–T21, T28, check E1, check E2, check E5 | pass |

## Exploratory checks

| ID | Action | Expected | Actual | Result |
| --- | --- | --- | --- | --- |
| E1 | Run `./scripts/run.sh` from repository root | Prints `=== Multimedia File Manager ===` and exits with code `0` without leaving persistence artifacts | Printed `=== Multimedia File Manager ===` and exited with `0` | pass |
| E2 | Populate 3 monitored directories (`audio_dir`, `images_dir`, `missing_dir`), delete `missing_dir` on disk, call `catalog.rescan()`, inspect `catalog.getStatistics()`, and mutate returned `int[]`, `long[]`, and `long[][]` arrays | `countsByExtension=[2, 1, 1, 1]`, `bytesByExtension=[400, 500, 1200, 800]`, `directoryExtensionMatrix=[[2, 1, 0, 0], [0, 0, 1, 1], [0, 0, 0, 0]]`, and subsequent getter calls remain unaffected by array mutations | Exact 1D/2D arrays returned and defensive copies preserved snapshot immutability | pass |
| E3 | Call `ReportExporter.export(List.of(), List.of(), emptyStats)` to a nested temporary report path | Creates UTF-8 report containing `(none)` in empty sections and leaves no `.tmp` sibling file | Report written with `(none)` markers and no `.tmp` file left behind | pass |
| E4 | Write binary files with unsupported `FORMAT_VERSION = 999` and negative directory count `-5`, then call `BinaryCatalogStorage.load()` | Both throw `StorageOperationException` without unhandled `EOFException` or `NegativeArraySizeException` | `StorageOperationException` thrown in both cases (`badVersionRejected=true`, `negCountRejected=true`) | pass |
| E5 | Instantiate `new MultimediaCatalog(new LocationRepository(tempRoot.resolve("locations.txt")))`, call `exportReport()` and `saveAllOnExit()`, and inspect `loadBinarySnapshot()` | Default `reports/multimedia-report.txt` and `data/catalog.bin` are created under `tempRoot` (not repository root), and `loadBinarySnapshot()` restores 3 directories (with 3rd as `MISSING`) and 5 files | Both files created under `tempRoot` and binary snapshot restored `loadedDirs=3, loadedFiles=5` with `MISSING` status | pass |

## Failures

| ID | Description | Steps to reproduce | Severity |
| --- | --- | --- | --- |
| None | - | - | - |

## Known issues to record

| Description | Severity |
| --- | --- |
| None | - |

## Decision

pass: continue with task-gitops.
