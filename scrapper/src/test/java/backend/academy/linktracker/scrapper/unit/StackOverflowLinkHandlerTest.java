package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.clients.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowItemFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowOwnerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowTimelineFetchResult;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangePreviewFormatter;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.stackoverflow.StackOverflowLinkHandler;
import backend.academy.linktracker.scrapper.handlers.stackoverflow.StackOverflowTimelineChangeBuilder;
import backend.academy.linktracker.scrapper.handlers.stackoverflow.StackOverflowTimelineSupport;
import backend.academy.linktracker.scrapper.link.parser.StackOverflowQuestionLinkParser;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StackOverflowLinkHandlerTest {

    @Mock
    private StackOverflowTrackingStateRepository repository;

    @Mock
    private StackOverflowClient stackOverflowClient;

    @Mock
    private StackOverflowQuestionLinkParser stackOverflowQuestionLinkParser;

    private StackOverflowLinkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StackOverflowLinkHandler(
                repository,
                stackOverflowClient,
                new StackOverflowTimelineSupport(),
                new StackOverflowTimelineChangeBuilder(new LinkChangePreviewFormatter()),
                stackOverflowQuestionLinkParser);
    }

    @Test
    void checkForUpdate_shouldIgnoreNonTrackedTimelineEvents() {
        TrackedLink trackedLink = trackedLink("https://stackoverflow.com/questions/12345678/example");
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setLastQuestionActivityDateEpochSec(100L);

        StackOverflowQuestionResponse question = new StackOverflowQuestionResponse(12345678L, 200L, "Example");
        StackOverflowQuestionTimelineEventResponse revision =
                new StackOverflowQuestionTimelineEventResponse(200L, "revision", 12345678L, 10L, null, "rev-1");

        when(repository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(stackOverflowClient.fetchQuestion(any(StackOverflowQuestionKey.class)))
                .thenReturn(new StackOverflowQuestionFetchResult(question, 0));
        when(stackOverflowClient.fetchQuestionTimeline(any(StackOverflowQuestionKey.class), anyInt()))
                .thenReturn(new StackOverflowTimelineFetchResult(List.of(revision), 0));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isEmpty();
        verify(repository).save(any(StackOverflowTrackingState.class));
        verify(stackOverflowClient, never()).fetchAnswer(anyLong());
        verify(stackOverflowClient, never()).fetchComment(anyLong());
    }

    @Test
    void checkForUpdate_shouldReturnCommentChangeWhenNewCommentAppears() {
        TrackedLink trackedLink = trackedLink("https://stackoverflow.com/questions/12345678/example");
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setLastQuestionActivityDateEpochSec(100L);

        StackOverflowQuestionResponse question = new StackOverflowQuestionResponse(12345678L, 200L, "Example");
        StackOverflowQuestionTimelineEventResponse comment =
                new StackOverflowQuestionTimelineEventResponse(200L, "comment", 12345678L, 10L, 33L, null);
        StackOverflowCommentResponse commentDetails = new StackOverflowCommentResponse(
                33L, 200L, "<p>Hello&nbsp;<b>world</b> &amp; bye</p>", new StackOverflowOwnerResponse("alice"));

        when(repository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(stackOverflowClient.fetchQuestion(any(StackOverflowQuestionKey.class)))
                .thenReturn(new StackOverflowQuestionFetchResult(question, 0));
        when(stackOverflowClient.fetchQuestionTimeline(any(StackOverflowQuestionKey.class), anyInt()))
                .thenReturn(new StackOverflowTimelineFetchResult(List.of(comment), 0));
        when(stackOverflowClient.fetchComment(33L)).thenReturn(new StackOverflowItemFetchResult<>(commentDetails, 0));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getType()).isEqualTo(LinkChangeType.STACKOVERFLOW_COMMENT);
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New StackOverflow comment");
        assertThat(result.orElseThrow().getTitle()).isEqualTo("Example");
        assertThat(result.orElseThrow().getUsername()).isEqualTo("alice");
        assertThat(result.orElseThrow().getCreatedAt()).isEqualTo(Instant.ofEpochSecond(200L));
        assertThat(result.orElseThrow().getPreview()).isEqualTo("Hello world & bye");
        verify(repository).save(any(StackOverflowTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldReturnAnswerChangeWhenNewAnswerAppears() {
        TrackedLink trackedLink = trackedLink("https://stackoverflow.com/questions/12345678/example");
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setLastQuestionActivityDateEpochSec(100L);

        StackOverflowQuestionResponse question = new StackOverflowQuestionResponse(12345678L, 200L, "Example");
        StackOverflowQuestionTimelineEventResponse answer =
                new StackOverflowQuestionTimelineEventResponse(200L, "answer", 12345678L, 44L, null, null);
        StackOverflowAnswerResponse answerDetails = new StackOverflowAnswerResponse(
                44L, 200L, "<p>Answer with <code>&lt;tag&gt;</code></p>", new StackOverflowOwnerResponse("bob"));

        when(repository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(stackOverflowClient.fetchQuestion(any(StackOverflowQuestionKey.class)))
                .thenReturn(new StackOverflowQuestionFetchResult(question, 0));
        when(stackOverflowClient.fetchQuestionTimeline(any(StackOverflowQuestionKey.class), anyInt()))
                .thenReturn(new StackOverflowTimelineFetchResult(List.of(answer), 0));
        when(stackOverflowClient.fetchAnswer(44L)).thenReturn(new StackOverflowItemFetchResult<>(answerDetails, 0));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getType()).isEqualTo(LinkChangeType.STACKOVERFLOW_ANSWER);
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New StackOverflow answer");
        assertThat(result.orElseThrow().getTitle()).isEqualTo("Example");
        assertThat(result.orElseThrow().getUsername()).isEqualTo("bob");
        assertThat(result.orElseThrow().getCreatedAt()).isEqualTo(Instant.ofEpochSecond(200L));
        assertThat(result.orElseThrow().getPreview()).isEqualTo("Answer with <tag>");
        verify(repository).save(any(StackOverflowTrackingState.class));
    }

    @Test
    void checkForUpdate_shouldNotNotifyWhenQuestionUnavailable() {
        TrackedLink trackedLink = trackedLink("https://stackoverflow.com/questions/12345678/example");
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setLastQuestionActivityDateEpochSec(100L);

        when(repository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(stackOverflowClient.fetchQuestion(any(StackOverflowQuestionKey.class)))
                .thenReturn(new StackOverflowQuestionFetchResult(null, 0));

        Optional<LinkChange> result = handler.checkForUpdate(trackedLink);

        assertThat(result).isEmpty();
        verify(repository).save(any(StackOverflowTrackingState.class));
        verify(stackOverflowClient, never()).fetchQuestionTimeline(any(StackOverflowQuestionKey.class), anyInt());
        verify(stackOverflowClient, never()).fetchAnswer(anyLong());
        verify(stackOverflowClient, never()).fetchComment(anyLong());
    }

    @Test
    void checkForUpdate_shouldThrowWhenCommentDetailsEndpointFails() {
        TrackedLink trackedLink = trackedLink("https://stackoverflow.com/questions/12345678/example");
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setLastQuestionActivityDateEpochSec(100L);

        StackOverflowQuestionResponse question = new StackOverflowQuestionResponse(12345678L, 200L, "Example");
        StackOverflowQuestionTimelineEventResponse comment =
                new StackOverflowQuestionTimelineEventResponse(200L, "comment", 12345678L, 10L, 33L, null);

        when(repository.findByTrackedLink(trackedLink)).thenReturn(Optional.of(state));
        when(stackOverflowClient.fetchQuestion(any(StackOverflowQuestionKey.class)))
                .thenReturn(new StackOverflowQuestionFetchResult(question, 0));
        when(stackOverflowClient.fetchQuestionTimeline(any(StackOverflowQuestionKey.class), anyInt()))
                .thenReturn(new StackOverflowTimelineFetchResult(List.of(comment), 0));
        when(stackOverflowClient.fetchComment(33L))
                .thenThrow(new RepositoryPollingException("Failed to call StackOverflow API for comment 33"));

        assertThatThrownBy(() -> handler.checkForUpdate(trackedLink))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("comment 33");

        verify(repository, never()).save(any(StackOverflowTrackingState.class));
    }

    private TrackedLink trackedLink(String url) {
        return new TrackedLink(1L, url, new StackOverflowQuestionKey(12345678L));
    }
}
