package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import java.util.Optional;

public interface GitHubTrackingStateRepository {
    boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState);

    GitHubTrackingState save(GitHubTrackingState gitHubTrackingState);

    void deleteByTrackedLink(TrackedLink trackedLink);

    Optional<GitHubTrackingState> findByTrackedLink(TrackedLink trackedLink);

    void clear();
}
