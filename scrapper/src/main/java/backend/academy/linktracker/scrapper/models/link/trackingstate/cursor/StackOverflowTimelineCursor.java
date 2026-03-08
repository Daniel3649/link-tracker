package backend.academy.linktracker.scrapper.models.link.trackingstate.cursor;

public record StackOverflowTimelineCursor(
    long lastQuestionActivityDateEpochSec,
    long lastCreationDateEpochSec,
    String lastEventKey
) {
    public static StackOverflowTimelineCursor empty() {
        return new StackOverflowTimelineCursor(0L, 0L, null);
    }
}
