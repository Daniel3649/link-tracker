package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackingState;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import java.net.URI;
import java.util.Optional;

public interface LinkHandler {
    boolean supports(URI uri);

    ParsedLink parse(URI uri);

    PreparedTrackingState prepareTrackingState(ParsedLink parsedLink);

    default void createTrackingState(TrackedLink trackedLink) {
        prepareTrackingState(new ParsedLink(trackedLink.getUrl(), trackedLink.getResourceKey())).persist(trackedLink);
    }

    void deleteTrackingStateIfExists(TrackedLink trackedLink);

    Optional<LinkChange> checkForUpdate(TrackedLink trackedLink);
}
