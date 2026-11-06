---
task: multimedia-scanner-filter
artifact: test-report
round: 1
tested_commit: a6eef74
verdict: pass
---

# Test report: multimedia-scanner-filter, round 1

## Environment

- OpenJDK 21.0.12.1 (64-Bit Server VM, Ubuntu), Linux 6.18.54.1-microsoft-standard-WSL2 x86_64, local filesystem (`java.nio.file`)

## Automated tests

- Clean install and test command: 21 passed, 0 failed, 0 skipped (`./scripts/build.sh && ./scripts/lint.sh && ./scripts/test.sh`)

| Plan test | Exists | Result |
| --- | --- | --- |
| T1 | yes | pass |
| T2 | yes | pass |
| T3 | yes | pass |
| T4 | yes | pass |
| T5 | yes | pass |
| T6 | yes | pass |
| T7 | yes | pass |
| T8 | yes | pass |
| T9 | yes | pass |
| T10 | yes | pass |
| T11 | yes | pass |
| T12 | yes | pass |
| T13 | yes | pass |
| T14 | yes | pass |
| T15 | yes | pass |
| T16 | yes | pass |
| T17 | yes | pass |
| T18 | yes | pass |
| T19 | yes | pass |
| T20 | yes | pass |
| T21 | yes | pass |

## Acceptance criteria

| Requirement | Verified by | Result |
| --- | --- | --- |
| R1 | T17, check E1, check E2 | pass |
| R2 | T18, check E3, check E5 | pass |
| R3 | T19, check E4, check E5 | pass |
| R4 | T20, check E4, check E5 | pass |
| R5 | T21, check E4, check E5 | pass |

## Exploratory checks

| ID | Action | Expected | Actual | Result |
| --- | --- | --- | --- | --- |
| E1 | Scan a temporary nested directory tree (`dirA/` and `dirA/sub/`) containing `Track_01.MP3` (120B), `Holiday.Jpg` (250B), `alert.wav` (80B), `logo.PNG` (64B), `notes.txt` (40B), and a directory symlink `symlink_sub -> sub` via `MultimediaScanner.scanDirectory(dirA)` | Returns 4 `MultimediaFile` items sorted by `fullPath` (`Track_01.MP3`, `logo.PNG`, `Holiday.Jpg`, `alert.wav`), ignores `notes.txt`, does not follow `symlink_sub` (0 duplicates), 0 warnings, and leaves disk files unmodified | `E1_COUNT=4 WARNINGS=0`, exact 4 files in deterministic order with accurate normalized extensions, `MediaType`, and byte sizes | pass |
| E2 | Call `MultimediaScanner.scanDirectory` on a non-existent directory path and on `null` | Non-existent path returns 0 files and 1 non-fatal warning (`S6`); `null` throws `IllegalArgumentException` | `E2_MISSING_FILES=0 WARNINGS=1`, `E2_NULL=PASS:Directory path must not be null.` | pass |
| E3 | Initialize `MultimediaCatalog` from `locations.txt` with `dirA` and a missing directory, add overlapping subdirectory `dirA/sub`, then remove parent `dirA` | `initialize()` scans `dirA` (4 files) and reports 1 warning for missing directory; adding overlapping `sub` keeps 4 deduplicated files owned by `dirA`; removing `dirA` rescans `sub` (2 files) and keeps all disk files intact (`S3`) | `E3_INIT_FILES=4 WARNINGS=1`, `E3_OVERLAP_FILES=4 FIRST_OWNER=true`, `E3_AFTER_REMOVE_PARENT=2 DISK_TRACK1_EXISTS=true DISK_README_EXISTS=true` | pass |
| E4 | Query populated `MultimediaCatalog` with `filterByMediaType(AUDIO)`, `filterByMediaType(IMAGE)`, `filterByExtension("MP3")`, `filterByExtension(" .JPG ")`, and `searchByName("  holi ")` | Returns 2 audio files, 2 image files, 1 `.mp3` file, 1 `.jpg` file, and 1 filename match (`Holiday.Jpg`) | `E4_AUDIO=2 IMAGE=2 MP3=1 JPG=1 SEARCH=1` | pass |
| E5 | Pass invalid arguments (`null`, blank `"   "`, unsupported `".mp4"`) to `filterByMediaType`, `filterByExtension`, and `searchByName`, and attempt `.clear()` on `getMultimediaFiles()` and `getFilesByMediaType()` | All 6 invalid queries throw `IllegalArgumentException` and both collection getters throw `UnsupportedOperationException` on mutation (`8/8` checks) | `E5_VALIDATION_PASSES=8/8` | pass |
| E6 | Run `./scripts/run.sh` | Starts `ro.ase.multimedia.Main` cleanly and prints header | Printed `=== Multimedia File Manager ===` and exited with `0` | pass |

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
