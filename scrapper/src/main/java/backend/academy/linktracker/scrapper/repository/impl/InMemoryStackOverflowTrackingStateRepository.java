package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryStackOverflowTrackingStateRepository implements StackOverflowTrackingStateRepository {
    private final ConcurrentMap<TrackedLink, StackOverflowTrackingState> trackingStates = new ConcurrentHashMap<>();

    @Override
    public Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return Optional.ofNullable(trackingStates.get(trackedLink));
    }

    @Override
    public boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState) {
        return trackingStates.putIfAbsent(
            stackOverflowTrackingState.getTrackedLink(),
            stackOverflowTrackingState
        ) == null;
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
