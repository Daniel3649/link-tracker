package backend.academy.linktracker.scrapper.common;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;

@FunctionalInterface
public interface PreparedTrackingState {
    void persist(TrackedLink trackedLink);
}
