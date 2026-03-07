package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.handlers.link.ParsedLink;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import java.net.URI;

public interface LinkHandler {
    boolean supports(URI uri);

    ParsedLink parse(URI uri);

    void createTrackingState(TrackedLink trackedLink);
}
