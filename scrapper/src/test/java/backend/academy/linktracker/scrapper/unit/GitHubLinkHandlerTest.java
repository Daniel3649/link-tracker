package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.contract.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.github.GitHubActivityDescriptionBuilder;
import backend.academy.linktracker.scrapper.handlers.github.GitHubActivityExtractor;
import backend.academy.linktracker.scrapper.handlers.github.GitHubLinkHandler;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

@ExtendWith(MockitoExtension.class)
class GitHubLinkHandlerTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private GitHubTrackingStateRepository trackingStateRepository;

    @Mock
    private GitHubActivityExtractor activityExtractor;

    @Mock
    private GitHubActivityDescriptionBuilder descriptionBuilder;

    @Mock
    private GitHubRepositoryLinkParser gitHubRepositoryLinkParser;

    private GitHubLinkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GitHubLinkHandler(
                gitHubClient,
                trackingStateRepository,
                activityExtractor,
                descriptionBuilder,
                gitHubRepositoryLinkParser);
    }

    @Test
    void createTrackingState_shouldThrowRepositoryPollingException_whenGitHubReturnsNon2xx() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubRepositoryFetchResult fetchResult = mock(GitHubRepositoryFetchResult.class);
        when(fetchResult.isOk()).thenReturn(false);
        when(fetchResult.statusCode()).thenReturn(HttpStatusCode.valueOf(401));

        when(gitHubClient.fetchRepository(any(GitHubRepositoryKey.class), nullable(String.class)))
                .thenReturn(fetchResult);

        assertThatThrownBy(() -> handler.createTrackingState(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to initialize GitHub tracking state")
                .hasMessageContaining("401");

        verify(gitHubClient, never()).fetchRecentActivities(any(GitHubRepositoryKey.class), anyInt());
        verify(trackingStateRepository, never()).saveIfAbsent(any(GitHubTrackingState.class));
    }

    @Test
    void createTrackingState_shouldThrowRepositoryPollingException_whenGitHubResponseHasNoEtag() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubRepositoryFetchResult fetchResult = mock(GitHubRepositoryFetchResult.class);
        when(fetchResult.isOk()).thenReturn(true);
        when(fetchResult.etag()).thenReturn(null);

        when(gitHubClient.fetchRepository(any(GitHubRepositoryKey.class), nullable(String.class)))
                .thenReturn(fetchResult);

        assertThatThrownBy(() -> handler.createTrackingState(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("does not contain ETag");

        verify(gitHubClient, never()).fetchRecentActivities(any(GitHubRepositoryKey.class), anyInt());
        verify(trackingStateRepository, never()).saveIfAbsent(any(GitHubTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldThrowRepositoryPollingException_whenGitHubReturnsNon2xx() {
        TrackedLink trackedLink = trackedLink(
                "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World"));

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag("\"old-etag\"");
        state.setLastActivityId(1L);

        GitHubRepositoryFetchResult fetchResult = mock(GitHubRepositoryFetchResult.class);
        when(fetchResult.isNotModified()).thenReturn(false);
        when(fetchResult.isOk()).thenReturn(false);
        when(fetchResult.statusCode()).thenReturn(HttpStatusCode.valueOf(500));

        when(trackingStateRepository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(gitHubClient.fetchRepository(any(GitHubRepositoryKey.class), nullable(String.class)))
                .thenReturn(fetchResult);

        assertThatThrownBy(() -> handler.checkForUpdate(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to check GitHub repository")
                .hasMessageContaining("500");

        verify(gitHubClient, never()).fetchRecentActivities(any(GitHubRepositoryKey.class), anyInt());
    }

    private TrackedLink trackedLink(String url, GitHubRepositoryKey key) {
        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getUrl()).thenReturn(url);
        when(trackedLink.getResourceKey()).thenReturn(key);
        return trackedLink;
    }
}
