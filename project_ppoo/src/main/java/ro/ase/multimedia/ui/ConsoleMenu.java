package ro.ase.multimedia.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import ro.ase.multimedia.exception.MultimediaException;
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
        out.println("Monitored directories: " + catalog.getMonitoredDirectories().size());
    }

    private void handleAddDirectory() {
    }

    private void handleRemoveDirectory() {
    }

    private void handleDisplayAllFiles() {
    }

    private void handleFilterOrSearchFiles() {
    }

    private void handleViewStatisticsAndExport() {
    }
}
