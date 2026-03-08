package backend.academy.linktracker.scrapper.exception.link;

public class TrackingStateAlreadyExistsException extends RuntimeException {
    public TrackingStateAlreadyExistsException(String message) {
        super(message);
    }
}
