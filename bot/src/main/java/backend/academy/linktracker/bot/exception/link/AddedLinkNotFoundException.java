package backend.academy.linktracker.bot.exception.link;

public class AddedLinkNotFoundException extends RuntimeException {
    public AddedLinkNotFoundException(String message) {
        super(message);
    }
}
