package ro.ase.multimedia.exception;

/**
 * Thrown when reading or writing persistence or report files fails due to an underlying I/O error.
 */
public class StorageOperationException extends MultimediaException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new storage operation exception with the specified detail message.
     *
     * @param message the detail message describing the failed storage operation
     */
    public StorageOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new storage operation exception with the specified detail message and cause.
     *
     * @param message the detail message describing the failed storage operation
     * @param cause   the underlying I/O or storage cause
     */
    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
