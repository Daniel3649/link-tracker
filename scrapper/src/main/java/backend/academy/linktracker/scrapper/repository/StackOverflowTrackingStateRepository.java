package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import java.util.Optional;

public interface StackOverflowTrackingStateRepository {
    StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState);

    void deleteByTrackedLinkId(Long id);

    boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState);

    Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink);

    void clear();
}
