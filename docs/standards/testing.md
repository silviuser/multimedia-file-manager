# Testing standards

Scope: automated unit/integration tests in `src/test/java/` and manual verification scenarios. Rule IDs (`TEST-<n>`) are cited in plans, critiques and reviews.

## TEST-1. Automated self-contained test runner

Tests live in `src/test/java/ro/ase/multimedia/` and run with a single command (`./scripts/test.sh`) that compiles main and test classes and executes all test suites, exiting with `0` when all assertions pass and non-zero on any failure. No external internet downloads are required to run tests.

## TEST-2. Temporary directories for every file-system test (`S3`, `DATA-7`)

Every test that touches directories, `locations.txt`, `catalog.bin`, or `multimedia-report.txt` creates its own isolated temporary directory with `Files.createTempDirectory("multimedia-test-")` and cleans it up after the test. Tests never read or modify the project's root `locations.txt` or real user folders.

## TEST-3. Essential tests mapped to requirements

For each requirement in `scope.md`:
- One test for the success path (e.g., adding a valid directory updates memory and `locations.txt`; scanning finds `.mp3`, `.wav`, `.jpg`, `.png`; 1D and 2D primitive arrays match expected counts and byte totals);
- One test for each critical validation/safety rule (e.g., removing a directory leaves files on disk intact `S3`; adding a non-existent path throws `InvalidDirectoryException`; missing monitored directory is marked `MISSING` without crashing `S6`).

## TEST-4. End-to-end CLI flow tests with in-memory streams

`ConsoleMenu` tests feed scripted user commands via `ByteArrayInputStream` and inspect output captured in `ByteArrayOutputStream`, verifying the full startup -> add location -> list files -> filter -> statistics -> exit & save cycle (`G6`, `G7`).

## TEST-5. Descriptive test names

Test methods clearly state the scenario and expected outcome, for example `removeDirectory_deletesReferenceOnlyAndKeepsDiskFilesIntact` or `scan_populatesOneDimensionalAndTwoDimensionalPrimitiveArrays`.

## TEST-6. Existing tests keep passing

A task never deletes or weakens an existing test to make the suite pass. A test changes only when its requirement changed, and the plan states why.

## TEST-7. Repeatable manual verification

`README.md` documents step-by-step manual console verification commands so a user or evaluator can launch the app, exercise every menu option, and inspect `locations.txt`, `multimedia-report.txt`, and `catalog.bin`.
