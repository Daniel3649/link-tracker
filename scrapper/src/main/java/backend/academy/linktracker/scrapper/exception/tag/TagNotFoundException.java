package backend.academy.linktracker.scrapper.exception.tag;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String message) {
        super(message);
    }
}
