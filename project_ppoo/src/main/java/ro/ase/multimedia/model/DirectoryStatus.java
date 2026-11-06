package ro.ase.multimedia.model;

/**
 * Availability status of a monitored directory on the file system.
 */
public enum DirectoryStatus {
    /** Directory exists on disk, is a directory, and is readable. */
    ACCESSIBLE,
    /** Directory path does not exist on disk. */
    MISSING,
    /** Directory path exists on disk but is not a readable directory. */
    INACCESSIBLE
}
