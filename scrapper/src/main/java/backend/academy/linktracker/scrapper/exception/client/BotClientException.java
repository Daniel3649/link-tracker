package backend.academy.linktracker.scrapper.exception.client;

public class BotClientException extends RuntimeException {
    public BotClientException(String message) {
        super(message);
    }

    public BotClientException(String message, Throwable cause) {
    }
}
