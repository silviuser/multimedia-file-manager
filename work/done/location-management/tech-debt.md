---
task: location-management
artifact: tech-debt
---

# Tech debt: location-management

| ID | Location | Description | Kind | Removed by | Status |
| --- | --- | --- | --- | --- | --- |
| TD-location-management-1 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java:24` | Media file scanning (`MultimediaScanner`), 1D/2D array recalculation (`StatisticsCalculator`), and binary/report persistence (`BinaryCatalogStorage`, `ReportExporter`) are deferred to subsequent tasks | placeholder | `multimedia-scanner-filter`, `statistics-and-reports` | open |
