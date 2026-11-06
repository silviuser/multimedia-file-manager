# Git standards

Scope: branches, commits and pull requests. Rule IDs (`GIT-<n>`) are cited in plans, critiques and reviews.

## GIT-1. One branch per task

The branch has the task's slug as its name (`project-foundation`, `location-management`, etc.) and starts from an up-to-date `main`. `main` always compiles cleanly and passes all tests.

## GIT-2. Conventional Commits

`<type>(<scope>): <description>`, in the imperative, lowercase: `feat(storage): add text location repository`. Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`. Task files use `docs(<slug>): …`.

## GIT-3. Small commits

One logical step per commit; the build, lint and tests pass after every commit.

## GIT-4. Nothing generated or runtime-temporary in the repository

Compiled classes (`out/`, `target/`, `build/`, `*.class`), Javadoc output (`docs/javadoc/`, `target/site/`), IDE metadata (`.idea/`, `*.iml`, `.vscode/`), and runtime data/report files (`locations.txt`, `data/`, `reports/`, `*.bin`) are listed in `.gitignore`.

## GIT-5. Pull requests

One pull request per task, from the task branch to `main`. It is merged by a person, after the plan critique, the code review and the tests passed. Commits that were pushed are never rewritten.
