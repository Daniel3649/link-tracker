package backend.academy.linktracker.bot.exception.link;

public class LinkParsingException extends RuntimeException {
    public LinkParsingException(String message) {
        super(message);
    }

    public LinkParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
