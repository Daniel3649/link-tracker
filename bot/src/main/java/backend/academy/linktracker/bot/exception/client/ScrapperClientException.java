package backend.academy.linktracker.bot.exception.client;

public class ScrapperClientException extends RuntimeException {
    public ScrapperClientException(String message) {
        super(message);
    }

    public ScrapperClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
