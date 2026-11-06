package ro.ase.multimedia.exception;

/**
 * Base unchecked domain exception for all Multimedia File Manager errors.
 */
public class MultimediaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new multimedia exception with the specified detail message.
     *
     * @param message the detail message explaining the failure
     */
    public MultimediaException(String message) {
        super(message);
    }

    /**
     * Constructs a new multimedia exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the failure
     * @param cause   the underlying cause of this exception
     */
    public MultimediaException(String message, Throwable cause) {
        super(message, cause);
    }
}
