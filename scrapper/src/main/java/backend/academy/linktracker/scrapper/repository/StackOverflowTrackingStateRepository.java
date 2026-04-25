package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import java.util.Optional;

public interface StackOverflowTrackingStateRepository {
    StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState);

    void deleteByTrackedLink(TrackedLink trackedLink);

    boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState);

    Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink);

    void clear();
}
