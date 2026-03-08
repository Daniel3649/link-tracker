package backend.academy.linktracker.scrapper.exception.chat;

public class TelegramChatAlreadyExistsException extends RuntimeException {
    public TelegramChatAlreadyExistsException(String message) {
        super(message);
    }
}
