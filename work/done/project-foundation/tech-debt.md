---
task: project-foundation
artifact: tech-debt
---

# Tech debt: project-foundation

| ID | Location | Description | Kind | Removed by | Status |
| --- | --- | --- | --- | --- | --- |
| TD-project-foundation-1 | `project_ppoo/src/main/java/ro/ase/multimedia/Main.java:18` | `Main.main` prints startup banner only; full interactive lifecycle requires `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu` | placeholder | `console-menu-app` | open |
