package ro.ase.multimedia.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.MultimediaException;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.service.MultimediaCatalog;

/**
 * Interactive console user interface driving the Multimedia File Manager lifecycle,
 * menu navigation, tabular output formatting, and user-friendly error presentation.
 */
public final class ConsoleMenu {

    private final MultimediaCatalog catalog;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean running;

    /**
     * Constructs a console menu bound to the given catalog and I/O streams.
     *
     * @param catalog the multimedia catalog service (must not be {@code null})
     * @param in      the input stream to read user commands from (must not be {@code null})
     * @param out     the print stream to write menu output and messages to (must not be {@code null})
     * @throws IllegalArgumentException if {@code catalog}, {@code in}, or {@code out} is {@code null}
     */
    public ConsoleMenu(MultimediaCatalog catalog, InputStream in, PrintStream out) {
        if (catalog == null) {
            throw new IllegalArgumentException("MultimediaCatalog must not be null.");
        }
        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("PrintStream must not be null.");
        }
        this.catalog = catalog;
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
        this.out = out;
    }

    /**
     * Runs the application lifecycle: initializes the catalog from persistent storage,
     * displays any startup warnings, processes menu commands until {@code 0} or EOF is reached,
     * and saves state on exit.
     */
    public void run() {
        out.println("=== Multimedia File Manager ===");
        try {
            List<String> warnings = catalog.initialize();
            for (String warning : warnings) {
                out.println("[WARN] " + warning);
            }
        } catch (MultimediaException ex) {
            out.println("[ERROR] " + ex.getMessage());
        }

        running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                running = false;
                break;
            }
            String trimmed = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(trimmed);
                switch (choice) {
                    case 1 -> handleViewDirectories();
                    case 2 -> handleAddDirectory();
                    case 3 -> handleRemoveDirectory();
                    case 4 -> handleDisplayAllFiles();
                    case 5 -> handleFilterOrSearchFiles();
                    case 6 -> handleViewStatisticsAndExport();
                    case 0 -> running = false;
                    default -> out.println("[ERROR] Invalid menu option: " + choice + ". Choose 0-6.");
                }
            } catch (NumberFormatException ex) {
                out.println("[ERROR] Invalid menu input: '" + trimmed + "'. Please enter a number (0-6).");
            } catch (MultimediaException | IllegalArgumentException ex) {
                out.println("[ERROR] " + ex.getMessage());
            }
        }

        try {
            catalog.saveAllOnExit();
            out.println("State saved. Exiting Multimedia File Manager.");
        } catch (MultimediaException ex) {
            out.println("[ERROR] " + ex.getMessage());
        }
    }

    private void printMenu() {
        out.println();
        out.println("--- Multimedia File Manager Menu ---");
        out.println("1 - Display monitored directories (path, status, file count)");
        out.println("2 - Add a new monitored directory");
        out.println("3 - Remove a monitored directory (keeps disk files intact)");
        out.println("4 - Display all multimedia files");
        out.println("5 - Filter or search multimedia files");
        out.println("6 - View statistics (1D/2D arrays) and export report / binary snapshot");
        out.println("0 - Save state and exit");
        out.print("Select option (0-6): ");
    }

    private void handleViewDirectories() {
        List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
        if (dirs.isEmpty()) {
            out.println("No monitored directories configured.");
            return;
        }
        long[][] matrix = catalog.getStatistics().getDirectoryExtensionMatrix();
        out.println("Monitored Directories (" + dirs.size() + "):");
        for (int i = 0; i < dirs.size(); i++) {
            MonitoredDirectory dir = dirs.get(i);
            long fileCount = 0L;
            if (i < matrix.length) {
                for (long count : matrix[i]) {
                    fileCount += count;
                }
            }
            out.println("[" + (i + 1) + "] " + dir.getPath()
                    + " | Status: " + dir.getStatus()
                    + " | Files: " + fileCount
                    + " | Added: " + dir.getAddedAt());
        }
    }

    private void handleAddDirectory() {
        out.print("Enter directory path to add: ");
        if (!scanner.hasNextLine()) {
            running = false;
            return;
        }
        String rawPath = scanner.nextLine();
        MonitoredDirectory added = catalog.addDirectory(rawPath);
        out.println("Added monitored directory: " + added.getPath());
    }

    private void handleRemoveDirectory() {
        out.print("Enter 1-based directory number or path to remove: ");
        if (!scanner.hasNextLine()) {
            running = false;
            return;
        }
        String rawInput = scanner.nextLine().trim();
        String targetPath = rawInput;
        try {
            int index = Integer.parseInt(rawInput);
            List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
            if (index < 1 || index > dirs.size()) {
                throw new LocationNotFoundException("Directory index out of range: " + index);
            }
            targetPath = dirs.get(index - 1).getPath().toString();
        } catch (NumberFormatException ignored) {
            // Input is a path string; pass directly to catalog.removeDirectory
        }
        MonitoredDirectory removed = catalog.removeDirectory(targetPath);
        out.println("Removed monitored directory reference (disk files kept intact): " + removed.getPath());
    }

    private void handleDisplayAllFiles() {
    }

    private void handleFilterOrSearchFiles() {
    }

    private void handleViewStatisticsAndExport() {
    }
}
