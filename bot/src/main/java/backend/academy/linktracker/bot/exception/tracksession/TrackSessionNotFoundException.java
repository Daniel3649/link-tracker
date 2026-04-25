package backend.academy.linktracker.bot.exception.tracksession;

public class TrackSessionNotFoundException extends RuntimeException {
    public TrackSessionNotFoundException(String message) {
        super(message);
    }
}
