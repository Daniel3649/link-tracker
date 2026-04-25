package backend.academy.linktracker.scrapper.exception.link;

public class NotFoundTrackedLinkException extends RuntimeException {
    public NotFoundTrackedLinkException(String message) {
        super(message);
    }
}
