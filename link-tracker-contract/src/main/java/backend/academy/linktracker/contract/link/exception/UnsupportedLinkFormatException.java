package backend.academy.linktracker.contract.link.exception;

public class UnsupportedLinkFormatException extends RuntimeException {
    public UnsupportedLinkFormatException(String message) {
        super(message);
    }

    public UnsupportedLinkFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
