package backend.academy.linktracker.scrapper.exception.link;

public class TrackingStateNotFoundException extends RuntimeException {
    public TrackingStateNotFoundException(String message) {
        super(message);
    }
}
