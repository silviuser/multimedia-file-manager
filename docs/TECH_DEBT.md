# Tech debt

Open shortcuts and placeholders. Each item comes from the `tech-debt.md` of a task or from a finding its code review accepted as debt; placeholders carry the same ID in a `TODO(<id>)` comment.

| ID | Location | Description | Removed by | Status |
| --- | --- | --- | --- | --- |
| TD-project-foundation-1 | `project_ppoo/src/main/java/ro/ase/multimedia/Main.java:18` | `Main.main` prints startup banner only; full interactive lifecycle requires `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu` | `console-menu-app` | open |
| TD-location-management-1 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java:24` | Media file scanning (`MultimediaScanner`), 1D/2D array recalculation (`StatisticsCalculator`), and binary/report persistence (`BinaryCatalogStorage`, `ReportExporter`) are deferred to subsequent tasks | `multimedia-scanner-filter`, `statistics-and-reports` | open |
