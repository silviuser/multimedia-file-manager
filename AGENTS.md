# AGENTS.md

Instructions for the coding agent working in this repository, for example Antigravity CLI (`agy`).

## Project

Multimedia File Manager (`Tema 4`): a Java 21 console application for monitoring directories on disk, cataloging and filtering multimedia files (`.mp3`, `.wav`, `.jpg`, `.png`), computing statistics in 1D/2D primitive arrays and object collections, and persisting state and reports to text (`locations.txt`, `reports/multimedia-report.txt`) and binary (`data/catalog.bin`) files. Read `README.md` and `docs/ARCHITECTURE.md` first, along with `general_req.txt` and `specif_req.txt`.

All code, comments, documents and commit messages are in English.

## Commands

The workflow skills use these commands by name. The first roadmap item (`project-foundation`) creates `./scripts/` with these commands so they work identically across Linux, WSL and Windows JDK installations.

| Name | Command | Notes |
| --- | --- | --- |
| install | `none` | standard Java 21 SDK (`java.nio.file`, `java.io`, `java.util`), no external runtime dependencies |
| build | `./scripts/build.sh` | compiles `src/main/java` and `src/test/java` into `out/` |
| test | `./scripts/test.sh` | compiles and runs all automated tests in `src/test/java` without interaction and exits |
| lint | `./scripts/lint.sh` | runs `javac -Xlint:all -Werror` and `javadoc` check; must report nothing |
| start | `./scripts/run.sh` | starts the interactive console application (`ro.ase.multimedia.Main`) |

Runtime persistence files (`locations.txt`, `data/catalog.bin`, `reports/multimedia-report.txt`) and compiled artifacts (`out/`, `target/`, `docs/javadoc/`) are never committed.

## Git

- Base branch: `main`. The skills call it `<base>`.
- Code commits follow `docs/standards/git.md`. Task files are committed as `docs(<slug>): …`.

## Project documents

| Document | Content | Written by |
| --- | --- | --- |
| `README.md` | what the software does, how to build, run and test it | task-doc-updates |
| `docs/ARCHITECTURE.md` | requirements traceability, components, data model, decisions | task-doc-updates |
| `docs/ROADMAP.md` | planned and finished increments | task-doc-updates |
| `docs/TECH_DEBT.md` | open shortcuts and placeholders | task-doc-updates |
| `docs/KNOWN_ISSUES.md` | known bugs and limitations | task-doc-updates |
| `docs/standards/*.md` | coding standards (`java`, `data`, `cli`, `validation`, `testing`, `git`), with rule IDs | humans only |

## Workflow

Every change is a task. task-scoping chooses its slug (kebab-case, for example `project-foundation`) and creates a branch with the same name and the directory `work/<slug>/`. When the task is finished, task-doc-updates moves the directory to `work/done/<slug>/`.

A task goes through eight phases. Each phase is a skill in `.agents/skills/` and runs in a new agent session.

| # | Skill | Reads | Writes | Continue when |
| --- | --- | --- | --- | --- |
| 1 | task-scoping | the user, project documents, the web | `scope.md` | the user confirms the scope |
| 2 | task-planning | `scope.md`, `critique.md`, documents, code | `plan.md` | always |
| 3 | task-plan-critique | `plan.md`, `scope.md`, documents, code | `critique.md` | verdict `approved` |
| 4 | task-development | approved `plan.md`, findings | code, tests, `dev-log.md`, `tech-debt.md` | tests pass |
| 5 | task-code-review | diff, `plan.md`, `dev-log.md`, `tech-debt.md` | `review.md` | verdict `approved` |
| 6 | task-testing | `scope.md`, `plan.md`, the running software | `test-report.md` | verdict `pass` |
| 7 | task-gitops | all task files, git | `pr.md`, pushed branch, pull request | branch pushed |
| 8 | task-doc-updates | all task files, project documents | project documents, `docs-update.md`; moves the task to `work/done/` | the user merges |

Loops: critique `changes-requested` goes back to task-planning; review `changes-requested` or test `fail` goes back to task-development, then to task-code-review again.

## Rules for every phase

1. One phase per session. Read this file, then the skill, then only the inputs the skill lists.
2. After task-scoping, every phase works on the task of the current branch: the slug is the branch name and the task files are in `work/<slug>/`. On `<base>`, or when that directory does not exist, stop and ask the user to switch to the task's branch. If only `work/done/<slug>/` exists, the task is closed: to change it, move the directory back (`git mv work/done/<slug> work/<slug>`), commit, and run the phase the change needs, usually task-development.
3. Write task files from the skill's template, in `assets/`. Keep the frontmatter; never edit a file that belongs to another phase.
4. Check the preconditions first. If one fails, stop and say what is missing.
5. When information is missing, ask the user; do not invent requirements.
6. Commit your phase's output on the task branch, adding each file explicitly (`git add <file>`). Only task-gitops and task-doc-updates push.
7. Keep task files short and precise: tables instead of prose, no text copied from other files (except in `pr.md`, which must stand alone). `scope.md` fits on about one page; `plan.md` stays under about 200 lines.
8. Check in proportion to the change: do not rerun a passing check without a change in between; no mutation testing and no prototypes of the planned code.
9. End with the next step, in the form `Next: <skill>`.

## Increment budget

A task is one logical increment: testable on its own, and every existing test still passes.

- At most about 300 changed lines of production code and 10 files; tests, documents and generated files do not count.
- At most one new runtime dependency; development tools do not count.
- Tests are essential, not exhaustive: one test for the success path of each requirement, plus one for each error case that protects security or data. A requirement usually needs one to three tests.

Larger work is split. A part that is not implemented yet is replaced by an explicit placeholder (a stub, a fake adapter, seeded dummy data, a monkey patch in tests), marked in code with `TODO(TD-<slug>-<n>)` and recorded in `work/<slug>/tech-debt.md`.
