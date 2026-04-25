package backend.academy.linktracker.bot.exception.client;

public class InvalidScrapperRequestException extends RuntimeException {
    public InvalidScrapperRequestException(String message) {
        super(message);
    }
}
