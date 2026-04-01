package backend.academy.linktracker.bot.exception.tracksession;

public class IllegalTrackStateException extends RuntimeException {
    public IllegalTrackStateException(String message) {
        super(message);
    }
}
