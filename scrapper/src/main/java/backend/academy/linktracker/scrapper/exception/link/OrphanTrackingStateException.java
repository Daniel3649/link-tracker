package backend.academy.linktracker.scrapper.exception.link;

public class OrphanTrackingStateException extends RuntimeException {
    public OrphanTrackingStateException(String message) {
        super(message);
    }
}
