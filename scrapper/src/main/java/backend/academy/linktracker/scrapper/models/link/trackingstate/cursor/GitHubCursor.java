package backend.academy.linktracker.scrapper.models.link.trackingstate.cursor;

public record GitHubCursor(String etag, String lastProcessedEventId){
}
