package ro.ase.multimedia.exception;

/**
 * Thrown when a candidate directory path is blank, invalid, missing on disk, not a directory, or unreadable.
 */
public class InvalidDirectoryException extends MultimediaException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new invalid directory exception with the specified detail message.
     *
     * @param message the detail message explaining why the directory path is invalid
     */
    public InvalidDirectoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid directory exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why the directory path is invalid
     * @param cause   the underlying cause of this exception
     */
    public InvalidDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
