package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.contract.link.common.ParsedGitHubRepositoryLink;
import backend.academy.linktracker.contract.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryActivityResponse;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {
    private static final int ACTIVITY_FETCH_LIMIT = 10;

    private final GitHubClient gitHubClient;
    private final GitHubTrackingStateRepository trackingStateRepository;
    private final GitHubActivityExtractor activityExtractor;
    private final GitHubActivityDescriptionBuilder descriptionBuilder;
    private final GitHubRepositoryLinkParser gitHubRepositoryLinkParser;

    @Override
    public boolean supports(URI uri) {
        return gitHubRepositoryLinkParser.supports(uri);
    }

    @Override
    public ParsedLink parse(URI uri) {
        ParsedGitHubRepositoryLink parsed = gitHubRepositoryLinkParser.parse(uri);

        return new ParsedLink(parsed.uri().toString(), new GitHubRepositoryKey(parsed.owner(), parsed.repo()));
    }

    @Override
    public void createTrackingState(TrackedLink trackedLink) {
        GitHubRepositoryKey key = extractKey(trackedLink.getResourceKey());

        GitHubRepositoryFetchResult repositoryResult = gitHubClient.fetchRepository(key, null);
        if (!repositoryResult.isOk()) {
            throw new RepositoryPollingException("Failed to initialize GitHub tracking state for %s. HTTP status: %s"
                    .formatted(
                            trackedLink.getUrl(), repositoryResult.statusCode().value()));
        }

        String etag = requireEtag(repositoryResult, trackedLink.getUrl());
        List<GitHubRepositoryActivityResponse> recentActivities = gitHubClient.fetchRecentActivities(key, 1);

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag(etag);
        state.setLastActivityId(
                recentActivities.isEmpty() ? null : recentActivities.getFirst().id());

        boolean saved = trackingStateRepository.saveIfAbsent(state);
        if (!saved) {
            throw new TrackingStateAlreadyExistsException(
                    "Tracking state already exists for link: " + trackedLink.getUrl());
        }
    }

    @Override
    public void deleteTrackingState(TrackedLink trackedLink) {
        trackingStateRepository.deleteByTrackedLink(trackedLink);
    }

    @Override
    public Optional<LinkChange> checkForUpdate(TrackedLink trackedLink) {
        Optional<GitHubTrackingState> optionalState = trackingStateRepository.findByTrackedLink(trackedLink);
        if (optionalState.isEmpty()) {
            return Optional.empty();
        }

        GitHubTrackingState state = optionalState.get();
        GitHubRepositoryKey key = extractKey(trackedLink.getResourceKey());

        GitHubRepositoryFetchResult repositoryResult = gitHubClient.fetchRepository(key, state.getEtag());

        if (repositoryResult.isNotModified()) {
            return Optional.empty();
        }

        if (!repositoryResult.isOk()) {
            throw new RepositoryPollingException("Failed to check GitHub repository %s. HTTP status: %s"
                    .formatted(
                            trackedLink.getUrl(), repositoryResult.statusCode().value()));
        }

        String newEtag = requireEtag(repositoryResult, trackedLink.getUrl());

        List<GitHubRepositoryActivityResponse> recentActivities =
                gitHubClient.fetchRecentActivities(key, ACTIVITY_FETCH_LIMIT);

        List<GitHubRepositoryActivityResponse> newActivities =
                activityExtractor.extractNewActivities(recentActivities, state.getLastActivityId());

        state.setEtag(newEtag);
        if (!newActivities.isEmpty()) {
            state.setLastActivityId(newActivities.getFirst().id());
        }
        trackingStateRepository.save(state);

        if (newActivities.isEmpty()) {
            return Optional.of(new LinkChange("Repository changed: " + key.owner() + "/" + key.repo()));
        }

        return Optional.of(new LinkChange(descriptionBuilder.buildDescription(key, newActivities)));
    }

    private GitHubRepositoryKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof GitHubRepositoryKey key)) {
            throw new IllegalArgumentException("Resource key is not GitHubRepositoryKey");
        }
        return key;
    }

    private String requireEtag(GitHubRepositoryFetchResult result, String url) {
        if (!StringUtils.hasText(result.etag())) {
            throw new RepositoryPollingException("GitHub response does not contain ETag for link: " + url);
        }
        return result.etag();
    }
}
