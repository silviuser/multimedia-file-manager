/**
 * File-system persistence adapters for text configuration, formatted text
 * reports, and binary catalog snapshots.
 *
 * <p>Contains {@link ro.ase.multimedia.storage.LocationRepository}
 * ({@code locations.txt}), {@link ro.ase.multimedia.storage.ReportExporter}
 * ({@code reports/multimedia-report.txt}), and
 * {@link ro.ase.multimedia.storage.BinaryCatalogStorage}
 * ({@code data/catalog.bin}), using UTF-8 encoding, {@code try-with-resources},
 * and atomic sibling {@code .tmp} replacement without console I/O calls.
 */
package ro.ase.multimedia.storage;
