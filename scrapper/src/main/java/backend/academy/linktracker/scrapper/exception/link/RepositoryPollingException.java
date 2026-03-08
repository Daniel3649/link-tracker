package backend.academy.linktracker.scrapper.exception.link;

public class RepositoryPollingException extends RuntimeException {
    public RepositoryPollingException(String message) {
        super(message);
    }

    public RepositoryPollingException(String message, Throwable cause) {
        super(message, cause);
    }
}
