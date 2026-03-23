package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import java.util.Optional;

public interface GitHubTrackingStateRepository {
    boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState);

    GitHubTrackingState save(GitHubTrackingState gitHubTrackingState);

    void deleteByTrackedLinkId(Long id);

    Optional<GitHubTrackingState> findByTrackedLink(TrackedLink trackedLink);

    void clear();
}
