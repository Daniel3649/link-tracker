package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryStackOverflowTrackingStateRepository implements StackOverflowTrackingStateRepository {
    private final ConcurrentMap<TrackedLink, StackOverflowTrackingState> trackingStates = new ConcurrentHashMap<>();

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return trackingStates.containsKey(trackedLink);
    }

    @Override
    public StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState) {
        trackingStates.put(stackOverflowTrackingState.getTrackedLink(), stackOverflowTrackingState);
        return stackOverflowTrackingState;
    }

    @Override
    public void deleteByTrackedLink(TrackedLink trackedLink) {
        trackingStates.remove(trackedLink);
    }
}
