---
task: javadoc-and-documentation
artifact: scope
version: 1
status: confirmed
branch: javadoc-and-documentation
roadmap_item: javadoc-and-documentation
---

# Scope: Complete JavaDoc, Manual Verification Guide, and Course Project Documentation

## Goal

Developers and evaluators can generate complete package- and class-level HTML JavaDoc (`docs/javadoc/`) with zero `doclint` warnings (`G8`, `G9`, `JAVA-9`), execute repeatable step-by-step manual console verification scenarios from `README.md` (`TEST-7`), and export a complete Romanian academic project report (`docs/PROJECT_DOCUMENTATION.md`) to Microsoft Word (`.docx`) per course and seminar guidelines (`G9`).

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | Every application package (`ro.ase.multimedia`, `model`, `exception`, `storage`, `service`, `ui`) includes a `package-info.java` with package-level JavaDoc describing its responsibility and layer boundaries (`G8`, `G9`, `JAVA-1`, `JAVA-5`, `JAVA-9`). | Given all 6 `package-info.java` files and existing classes in `project_ppoo/src/main/java`, when `./scripts/lint.sh` runs `javac -Xlint:all -Werror` and `javadoc -encoding UTF-8 -Xdoclint:all -Werror`, then compilation and `javadoc` exit with code `0` and zero warnings. |
| R2 | `./scripts/javadoc.sh` generates complete UTF-8 HTML JavaDoc for all packages and public classes/methods into `docs/javadoc/` (which remains git-ignored per `GIT-4`) (`G9`, `JAVA-9`, `GIT-4`). | Given `./scripts/javadoc.sh`, when executed, then `docs/javadoc/index.html` and package summary HTML files for all 6 packages are generated with exit code `0` and `git status` shows no untracked files. |
| R3 | `README.md` documents the `./scripts/javadoc.sh` command and a repeatable step-by-step manual console verification scenario exercising options `1`–`6` and `0` and inspecting `locations.txt`, `reports/multimedia-report.txt`, and `data/catalog.bin` (`G6`, `G7`, `TEST-7`). | Given `README.md`, when an evaluator follows the manual verification section, then every command and input sequence for options `1`–`6` and `0` and the inspection steps for `locations.txt`, `reports/multimedia-report.txt`, and `data/catalog.bin` are explicitly documented. |
| R4 | `docs/PROJECT_DOCUMENTATION.md` provides the complete Romanian course documentation (`Tema 4`) structured for direct Microsoft Word (`.docx`) export, covering problem statement, requirements traceability (`G1`–`G9`, `S1`–`S6`), architecture & package/class JavaDoc reference, 1D/2D primitive arrays and collections, text/binary persistence, validation & exception hierarchy, console walkthrough, and Word/Pandoc export instructions (`G9`). | Given `docs/PROJECT_DOCUMENTATION.md`, when inspected or tested, then it exists as valid UTF-8 Markdown containing sections for all `G1`–`G9` and `S1`–`S6` requirements, all 6 packages and 17 classes, 1D/2D primitive arrays and collections, persistence formats, test scenarios, and `.docx` conversion instructions. |

## Out of scope

- Committing generated HTML files in `docs/javadoc/` or binary `.docx` files into git (`GIT-4`).
- Modifying existing business logic in `model`, `exception`, `storage`, `service`, or `ui` classes.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| `package-info.java` in all 6 packages + `./scripts/javadoc.sh` outputting to git-ignored `docs/javadoc/` | Standard Java 21 package documentation (`JAVA-9`), strict `-Xdoclint:all -Werror` validation, and one-command HTML output for course submission without polluting git (`GIT-4`) | Adds 6 small `package-info.java` files and 1 shell script | chosen |
| Legacy `package.html` files | Supported by older JDKs | Deprecated in favor of `package-info.java` in modern Java | rejected |
| Markdown source in `docs/PROJECT_DOCUMENTATION.md` (in Romanian per user selection for `G9`) with Pandoc / Word import instructions | Version-controlled text, diffs cleanly in git (`GIT-4`), and converts directly to `.docx` via Pandoc, Word 365, or VS Code | Requires a 1-step export or open-in-Word to produce the final `.docx` file | chosen |
| Committing a binary `.docx` file directly in the repository | No conversion step needed | Binary blobs cannot be diffed or code-reviewed cleanly in git | rejected |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/specs/javadoc/doc-comment-spec.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/javadoc.html
- https://pandoc.org/MANUAL.html

## Constraints and assumptions

- Code, JavaDoc comments, `README.md`, and git commits remain in English (`AGENTS.md`, `JAVA-1`); `docs/PROJECT_DOCUMENTATION.md` is written in Romanian as explicitly requested by the user to match the course submission language (`general_req.txt`, `specif_req.txt`, `G9`).
- `docs/javadoc/` is already listed in `.gitignore` (`GIT-4`), so running `./scripts/javadoc.sh` leaves the git working tree clean.
- Automated tests in `TestRunner.java` verify the presence and non-empty content of the 6 `package-info.java` files, `README.md` manual verification section, and `docs/PROJECT_DOCUMENTATION.md` required sections (`TEST-1`, `TEST-3`, `TEST-6`).

## Impact

- Code areas touched:
  - `project_ppoo/src/main/java/ro/ase/multimedia/package-info.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/model/package-info.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/exception/package-info.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/storage/package-info.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/service/package-info.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/ui/package-info.java` (new)
  - `scripts/javadoc.sh` (new)
  - `README.md` (updated)
  - `docs/PROJECT_DOCUMENTATION.md` (new)
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` (updated)
- Blast radius: low (adds package-level JavaDoc, documentation script, `README.md` manual scenarios, and `docs/PROJECT_DOCUMENTATION.md` without modifying runtime behavior).
- Increment budget: fits (6 small `package-info.java` files + 1 script = 7 files, ~100 lines of code; tests and documentation do not count against the 300-line production code budget).

## Split and placeholders

- None (fits within a single increment and completes the roadmap).

## Follow-ups

- None

## Open questions

- None
