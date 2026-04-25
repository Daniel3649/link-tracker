package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.github.GitHubLinkHandler;
import backend.academy.linktracker.scrapper.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GitHubLinkHandlerTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private GitHubTrackingStateRepository trackingStateRepository;

    @Mock
    private GitHubRepositoryLinkParser gitHubRepositoryLinkParser;

    private GitHubLinkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GitHubLinkHandler(gitHubClient, trackingStateRepository, gitHubRepositoryLinkParser);
    }

    @Test
    void createTrackingState_shouldThrowRepositoryPollingException_whenGitHubReturnsNon2xx() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubRepositoryFetchResult fetchResult = new GitHubRepositoryFetchResult(HttpStatus.UNAUTHORIZED);

        when(gitHubClient.fetchRepository(any(GitHubRepositoryKey.class))).thenReturn(fetchResult);

        assertThatThrownBy(() -> handler.createTrackingState(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to initialize GitHub tracking state")
                .hasMessageContaining("401");

        verify(gitHubClient, never()).fetchRecentIssuesAndPullRequests(any(GitHubRepositoryKey.class), anyInt());
        verify(trackingStateRepository, never()).saveIfAbsent(any(GitHubTrackingState.class));
    }

    @Test
    void createTrackingState_shouldStoreLatestIssueId_whenRepositoryExists() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubRepositoryFetchResult fetchResult = new GitHubRepositoryFetchResult(HttpStatus.OK);
        GitHubRepositoryIssueResponse issue =
                new GitHubRepositoryIssueResponse(101L, 7L, "Bug", "Body", Instant.now(), null, null);

        when(gitHubClient.fetchRepository(any(GitHubRepositoryKey.class))).thenReturn(fetchResult);
        when(gitHubClient.fetchRecentIssuesAndPullRequests(any(GitHubRepositoryKey.class), anyInt()))
                .thenReturn(List.of(issue));

        handler.createTrackingState(trackedLink);

        verify(trackingStateRepository).saveIfAbsent(any(GitHubTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldThrowRepositoryPollingException_whenIssuesEndpointFails() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setLastActivityId(1L);

        when(trackingStateRepository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(gitHubClient.fetchRecentIssuesAndPullRequests(any(GitHubRepositoryKey.class), anyInt()))
                .thenThrow(new RepositoryPollingException("Failed to fetch repository issues"));

        assertThatThrownBy(() -> handler.checkForUpdate(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to fetch repository issues");

        verify(trackingStateRepository, never()).save(any(GitHubTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldReturnIssueChangeWhenNewIssueAppears() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setLastActivityId(100L);

        GitHubRepositoryIssueResponse newIssue = new GitHubRepositoryIssueResponse(
                101L,
                8L,
                "Fix bug",
                "a".repeat(210),
                Instant.parse("2026-04-05T08:30:00Z"),
                new GitHubRepositoryIssueResponse.GitHubIssueUser("alice"),
                null);

        when(trackingStateRepository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(gitHubClient.fetchRecentIssuesAndPullRequests(any(GitHubRepositoryKey.class), anyInt()))
                .thenReturn(List.of(newIssue));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getType()).isEqualTo(LinkChangeType.GITHUB_ISSUE);
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New GitHub issue");
        assertThat(result.orElseThrow().getTitle()).isEqualTo("Fix bug");
        assertThat(result.orElseThrow().getUsername()).isEqualTo("alice");
        assertThat(result.orElseThrow().getCreatedAt()).isEqualTo(Instant.parse("2026-04-05T08:30:00Z"));
        assertThat(result.orElseThrow().getPreview()).isEqualTo("a".repeat(200));
        verify(trackingStateRepository).save(any(GitHubTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldReturnPullRequestChangeWhenNewPullRequestAppears() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setLastActivityId(100L);

        GitHubRepositoryIssueResponse newPullRequest = new GitHubRepositoryIssueResponse(
                101L,
                8L,
                "Add feature",
                "PR body",
                Instant.parse("2026-04-05T09:30:00Z"),
                new GitHubRepositoryIssueResponse.GitHubIssueUser("octocat"),
                new GitHubRepositoryIssueResponse.GitHubPullRequestMarker(
                        "https://api.github.com/repos/octocat/Hello-World/pulls/8"));

        when(trackingStateRepository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(gitHubClient.fetchRecentIssuesAndPullRequests(any(GitHubRepositoryKey.class), anyInt()))
                .thenReturn(List.of(newPullRequest));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getType()).isEqualTo(LinkChangeType.GITHUB_PULL_REQUEST);
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New GitHub pull request");
        assertThat(result.orElseThrow().getTitle()).isEqualTo("Add feature");
        assertThat(result.orElseThrow().getUsername()).isEqualTo("octocat");
        assertThat(result.orElseThrow().getCreatedAt()).isEqualTo(Instant.parse("2026-04-05T09:30:00Z"));
        assertThat(result.orElseThrow().getPreview()).isEqualTo("PR body");
        verify(trackingStateRepository).save(any(GitHubTrackingState.class));
    }

    private TrackedLink trackedLink(String url, GitHubRepositoryKey key) {
        return new TrackedLink(1L, url, key);
    }
}
