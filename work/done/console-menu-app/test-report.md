---
task: console-menu-app
artifact: test-report
round: 1
tested_commit: 9ccd877
verdict: pass
---

# Test report: console-menu-app, round 1

## Environment

- OpenJDK 21.0.12.1 (64-Bit Server VM, Ubuntu 24.04.4), Linux 6.18.54.1-microsoft-standard-WSL2 x86_64, local file system storage (`locations.txt`, `data/catalog.bin`, `reports/multimedia-report.txt`)

## Automated tests

- Clean install and test command (`./scripts/build.sh && ./scripts/lint.sh && ./scripts/test.sh`): 34 passed, 0 failed, 0 skipped

| Plan test | Exists | Result |
| --- | --- | --- |
| T1–T7, T9–T28 | yes | pass |
| T8 | yes | pass |
| T29 | yes | pass |
| T30 | yes | pass |
| T31 | yes | pass |
| T32 | yes | pass |
| T33 | yes | pass |
| T34 | yes | pass |

## Acceptance criteria

| Requirement | Verified by | Result |
| --- | --- | --- |
| R1 | T8, T29, T34, checks E1, E2, E3 | pass |
| R2 | T30, checks E1, E2 | pass |
| R3 | T31, check E1 | pass |
| R4 | T32, check E1 | pass |
| R5 | T8, T33, T34, check E3 | pass |

## Exploratory checks

| ID | Action | Expected | Actual | Result |
| --- | --- | --- | --- | --- |
| E1 | Run `./scripts/run.sh` with scripted CLI input (`1`, `4`, `2` adding two temporary directories with `.mp3`, `.wav`, `.jpg`, `.png` files, `1`, `4`, `5` filtering by `MediaType` `AUDIO`, extension `.png`, and keyword `sunset`, `6` viewing statistics/exporting, and `0` exit). | Prints empty states before adding directories, confirms both added directories, displays 1-based directory list (`ACCESSIBLE`, 2 files each), displays 4 files with human-readable + byte sizes, filters accurately in sub-options `1`–`3`, prints 1D/2D primitive array tables, creates `reports/multimedia-report.txt` and `data/catalog.bin`, and saves state on `0` exit. | All menu sections, filtered lists, and 1D/2D statistics printed as expected; `project_ppoo/locations.txt` (2 lines), `project_ppoo/reports/multimedia-report.txt` (7680 total bytes, 4 files), and `project_ppoo/data/catalog.bin` (886 bytes) written atomically. | pass |
| E2 | Delete the first monitored temporary directory on disk, then run `./scripts/run.sh` with `1` (view directories), `3` -> `1` (remove by 1-based index), `3` -> `<path>` (remove by path string), `1`, and EOF. | Prints `[WARN] Monitored directory is missing on disk: ...` at startup, shows `Status: MISSING | Files: 0` for `[1]` and `Status: ACCESSIBLE | Files: 2` for `[2]`, removes both references while keeping physical disk files in the second directory intact, and saves empty `locations.txt` and updated `catalog.bin` on EOF. | Startup `[WARN]` printed, statuses and counts matched, both directories removed via index and path string, `Sunset_Photo.jpg` and `icon.png` remained untouched on disk, and `locations.txt` flushed to 0 lines on EOF exit. | pass |
| E3 | Run `./scripts/run.sh` with invalid menu options (`xyz`, `99`, `-5`), blank and non-existent paths on `2`, out-of-range indices (`0`, `5`) on `3`, invalid filter options (`abc`, `8`), invalid `MediaType` (`VIDEO`), unsupported extension (`.mkv`), blank search query on `5`, and mid-prompt EOF after `2`. | Every invalid input prints `[ERROR] <message>` without raw Java exception stack traces, the menu loop continues, and mid-prompt EOF triggers `State saved. Exiting Multimedia File Manager.`. | All 11 invalid inputs printed descriptive `[ERROR]` messages with zero stack traces, and mid-prompt EOF exited cleanly after saving state. | pass |

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
