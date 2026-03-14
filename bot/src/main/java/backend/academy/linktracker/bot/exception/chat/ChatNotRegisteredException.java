package backend.academy.linktracker.bot.exception.chat;

public class ChatNotRegisteredException extends RuntimeException {
    public ChatNotRegisteredException(String message) {
        super(message);
    }
}
