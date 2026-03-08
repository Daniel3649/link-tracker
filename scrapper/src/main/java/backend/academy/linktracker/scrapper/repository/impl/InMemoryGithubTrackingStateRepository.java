package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryGithubTrackingStateRepository implements GitHubTrackingStateRepository {
    private final ConcurrentMap<TrackedLink, GitHubTrackingState> trackingStates = new ConcurrentHashMap<>();

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return trackingStates.containsKey(trackedLink);
    }

    @Override
    public GitHubTrackingState save(GitHubTrackingState gitHubTrackingState) {
        trackingStates.put(gitHubTrackingState.getTrackedLink(), gitHubTrackingState);
        return gitHubTrackingState;
    }

    @Override
    public void deleteByTrackedLink(TrackedLink trackedLink) {
        trackingStates.remove(trackedLink);
    }
}
