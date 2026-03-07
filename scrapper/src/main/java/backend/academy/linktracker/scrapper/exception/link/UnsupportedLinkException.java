package backend.academy.linktracker.scrapper.exception.link;

public class UnsupportedLinkException extends RuntimeException {
    public UnsupportedLinkException(String message) {
        super(message);
    }

    public UnsupportedLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
