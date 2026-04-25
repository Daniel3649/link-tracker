package backend.academy.linktracker.bot.exception.chat;

public class ChatAlreadyRegisteredException extends RuntimeException {
    public ChatAlreadyRegisteredException(String message) {
        super(message);
    }
}
