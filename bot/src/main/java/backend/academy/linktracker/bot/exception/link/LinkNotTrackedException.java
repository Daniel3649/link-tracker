package backend.academy.linktracker.bot.exception.link;

public class LinkNotTrackedException extends RuntimeException {
    public LinkNotTrackedException(String message) {
        super(message);
    }
}
