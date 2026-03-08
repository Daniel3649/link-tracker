package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;

public interface StackOverflowTrackingStateRepository {
    boolean existsByTrackedLink(TrackedLink trackedLink);
    StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState);
    void deleteByTrackedLink(TrackedLink trackedLink);
}
