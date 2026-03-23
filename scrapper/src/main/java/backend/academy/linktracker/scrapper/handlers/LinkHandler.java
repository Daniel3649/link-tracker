package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import java.net.URI;
import java.util.Optional;

public interface LinkHandler {
    boolean supports(URI uri);

    ParsedLink parse(URI uri);

    void createTrackingState(TrackedLink trackedLink);

    void deleteTrackingStateIfExists(TrackedLink trackedLink);

    Optional<LinkChange> checkForUpdate(TrackedLink trackedLink);
}
