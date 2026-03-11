package backend.academy.linktracker.bot.exception.client;

public class ScrapperUnavailableException extends RuntimeException {
    public ScrapperUnavailableException(String message) {
        super(message);
    }

    public ScrapperUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
