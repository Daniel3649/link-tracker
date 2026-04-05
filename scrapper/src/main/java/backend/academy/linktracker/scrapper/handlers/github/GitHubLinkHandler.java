package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackingState;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {
    private static final int ISSUE_FETCH_LIMIT = 10;

    private final GitHubClient gitHubClient;
    private final GitHubTrackingStateRepository trackingStateRepository;
    private final GitHubIssueExtractor issueExtractor;
    private final GitHubIssueChangeBuilder changeBuilder;
    private final GitHubRepositoryLinkParser gitHubRepositoryLinkParser;

    @Override
    public boolean supports(URI uri) {
        return gitHubRepositoryLinkParser.supports(uri);
    }

    @Override
    public ParsedLink parse(URI uri) {
        return gitHubRepositoryLinkParser.parse(uri);
    }

    @Override
    public PreparedTrackingState prepareTrackingState(ParsedLink parsedLink) {
        GitHubRepositoryKey key = extractKey(parsedLink.resourceKey());

        GitHubRepositoryFetchResult repositoryResult = gitHubClient.fetchRepository(key);
        if (!repositoryResult.isOk()) {
            throw new RepositoryPollingException("Failed to initialize GitHub tracking state for %s. HTTP status: %s"
                    .formatted(
                            parsedLink.url(), repositoryResult.statusCode().value()));
        }

        List<GitHubRepositoryIssueResponse> recentIssues = gitHubClient.fetchRecentIssuesAndPullRequests(key, 1);
        Long lastActivityId = recentIssues.isEmpty() ? null : recentIssues.getFirst().id();

        return trackedLink -> {
            GitHubTrackingState state = new GitHubTrackingState(trackedLink);
            state.setLastActivityId(lastActivityId);
            trackingStateRepository.saveIfAbsent(state);
        };
    }

    @Override
    public void deleteTrackingStateIfExists(TrackedLink trackedLink) {
        trackingStateRepository.deleteByTrackedLinkId(trackedLink.getId());
    }

    @Override
    public Optional<LinkChange> checkForUpdate(TrackedLink trackedLink) {
        Optional<GitHubTrackingState> optionalState = trackingStateRepository.findByTrackedLink(trackedLink);
        if (optionalState.isEmpty()) {
            return Optional.empty();
        }

        GitHubTrackingState state = optionalState.orElseThrow();
        GitHubRepositoryKey key = extractKey(trackedLink.getResourceKey());

        List<GitHubRepositoryIssueResponse> recentIssues =
                gitHubClient.fetchRecentIssuesAndPullRequests(key, ISSUE_FETCH_LIMIT);
        List<GitHubRepositoryIssueResponse> newIssues =
                issueExtractor.extractNewIssuesOrPullRequests(recentIssues, state.getLastActivityId());

        if (!recentIssues.isEmpty()) {
            state.setLastActivityId(recentIssues.getFirst().id());
            trackingStateRepository.save(state);
        }

        if (newIssues.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(changeBuilder.buildChange(key, newIssues));
    }

    private GitHubRepositoryKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof GitHubRepositoryKey key)) {
            throw new IllegalArgumentException("Resource key is not GitHubRepositoryKey");
        }
        return key;
    }
}
