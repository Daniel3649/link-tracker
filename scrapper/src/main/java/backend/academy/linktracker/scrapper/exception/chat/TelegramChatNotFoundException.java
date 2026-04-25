package backend.academy.linktracker.scrapper.exception.chat;

public class TelegramChatNotFoundException extends RuntimeException {
    public TelegramChatNotFoundException(String message) {
        super(message);
    }
}
