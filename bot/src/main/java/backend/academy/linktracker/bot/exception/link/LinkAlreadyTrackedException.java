package backend.academy.linktracker.bot.exception.link;

public class LinkAlreadyTrackedException extends RuntimeException {
    public LinkAlreadyTrackedException(String message) {
        super(message);
    }
}
