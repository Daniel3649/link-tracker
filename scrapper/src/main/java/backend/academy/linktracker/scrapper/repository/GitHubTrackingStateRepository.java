package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;

public interface GitHubTrackingStateRepository {
    boolean existsByTrackedLink(TrackedLink trackedLink);
    GitHubTrackingState save(GitHubTrackingState gitHubTrackingState);
    void deleteByTrackedLink(TrackedLink trackedLink);
}
