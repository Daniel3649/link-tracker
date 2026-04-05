package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowItemFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowTimelineFetchResult;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackingState;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.cursor.StackOverflowTimelineCursor;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.link.parser.StackOverflowQuestionLinkParser;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackOverflowLinkHandler implements LinkHandler {
    private static final int TIMELINE_FETCH_LIMIT = 10;

    private final StackOverflowTrackingStateRepository repository;
    private final StackOverflowClient stackOverflowClient;
    private final StackOverflowTimelineSupport timelineSupport;
    private final StackOverflowTimelineChangeBuilder changeBuilder;
    private final StackOverflowQuestionLinkParser stackOverflowQuestionLinkParser;

    @Override
    public boolean supports(URI uri) {
        return stackOverflowQuestionLinkParser.supports(uri);
    }

    @Override
    public ParsedLink parse(URI uri) {
        return stackOverflowQuestionLinkParser.parse(uri);
    }

    @Override
    public PreparedTrackingState prepareTrackingState(ParsedLink parsedLink) {
        StackOverflowQuestionKey key = extractKey(parsedLink.resourceKey());

        StackOverflowQuestionFetchResult questionResult = stackOverflowClient.fetchQuestion(key);
        if (questionResult.question() == null) {
            throw new RepositoryPollingException(
                    "Failed to initialize StackOverflow tracking state for %s: question not found"
                            .formatted(parsedLink.url()));
        }

        StackOverflowTimelineFetchResult timelineResult = stackOverflowClient.fetchQuestionTimeline(key, 1);
        return trackedLink -> {
            StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
            state.setTimelineCursor(timelineSupport.buildInitialCursor(timelineResult.events()));
            state.setNextCheckAt(timelineSupport.calculateNextCheckAt(
                    questionResult.backoffSeconds(), timelineResult.backoffSeconds()));
            state.setLastQuestionActivityDateEpochSec(questionResult.question().lastActivityDateEpochSec());
            repository.saveIfAbsent(state);
        };
    }

    @Override
    public void deleteTrackingStateIfExists(TrackedLink trackedLink) {
        repository.deleteByTrackedLinkId(trackedLink.getId());
    }

    @Override
    public Optional<LinkChange> checkForUpdate(TrackedLink trackedLink) {
        StackOverflowTrackingState state =
                repository.findByTrackedLink(trackedLink).orElse(null);

        if (state == null) {
            return Optional.empty();
        }

        if (state.getNextCheckAt() != null && Instant.now().isBefore(state.getNextCheckAt())) {
            return Optional.empty();
        }

        StackOverflowQuestionKey key = extractKey(trackedLink.getResourceKey());
        StackOverflowTimelineCursor cursor =
                state.getTimelineCursor() != null ? state.getTimelineCursor() : StackOverflowTimelineCursor.empty();

        StackOverflowQuestionFetchResult questionResult = stackOverflowClient.fetchQuestion(key);
        StackOverflowQuestionResponse question = questionResult.question();

        if (question == null) {
            state.setNextCheckAt(timelineSupport.calculateNextCheckAt(questionResult.backoffSeconds(), null));
            repository.save(state);
            return Optional.empty();
        }

        Long currentLastActivity = question.lastActivityDateEpochSec();
        if (currentLastActivity != null && currentLastActivity <= state.getLastQuestionActivityDateEpochSec()) {
            state.setNextCheckAt(timelineSupport.calculateNextCheckAt(questionResult.backoffSeconds(), null));
            repository.save(state);
            return Optional.empty();
        }

        StackOverflowTimelineFetchResult timelineResult =
                stackOverflowClient.fetchQuestionTimeline(key, TIMELINE_FETCH_LIMIT);

        List<StackOverflowQuestionTimelineEventResponse> newEvents =
                timelineSupport.extractNewEvents(timelineResult.events(), cursor);
        List<StackOverflowQuestionTimelineEventResponse> trackedEvents =
                timelineSupport.extractTrackedEvents(newEvents);

        state.setTimelineCursor(timelineSupport.buildUpdatedCursor(timelineResult.events(), cursor));
        state.setLastQuestionActivityDateEpochSec(timelineSupport.safeLong(currentLastActivity));

        if (trackedEvents.isEmpty()) {
            state.setNextCheckAt(timelineSupport.calculateNextCheckAt(
                    questionResult.backoffSeconds(), timelineResult.backoffSeconds()));
            repository.save(state);
            return Optional.empty();
        }

        TrackedChangeBuildResult changeResult = buildTrackedChange(question, trackedEvents);
        state.setNextCheckAt(timelineSupport.calculateNextCheckAt(
                questionResult.backoffSeconds(), timelineResult.backoffSeconds(), changeResult.backoffSeconds()));
        repository.save(state);

        return Optional.of(changeResult.change());
    }

    private StackOverflowQuestionKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof StackOverflowQuestionKey key)) {
            throw new IllegalArgumentException("Resource key is not StackOverflowQuestionKey");
        }
        return key;
    }

    private TrackedChangeBuildResult buildTrackedChange(
            StackOverflowQuestionResponse question, List<StackOverflowQuestionTimelineEventResponse> trackedEvents) {
        StackOverflowQuestionTimelineEventResponse newestEvent = trackedEvents.getFirst();

        if (isCommentEvent(newestEvent)) {
            Long commentId = newestEvent.commentId();
            if (commentId == null) {
                throw new RepositoryPollingException("StackOverflow comment event does not contain comment id for %s"
                        .formatted(question.questionId()));
            }

            StackOverflowItemFetchResult<StackOverflowCommentResponse> commentResult =
                    stackOverflowClient.fetchComment(commentId);
            if (commentResult.item() == null) {
                throw new RepositoryPollingException(
                        "Failed to fetch StackOverflow comment %s for %s".formatted(commentId, question.questionId()));
            }

            return new TrackedChangeBuildResult(
                    changeBuilder.buildCommentChange(question, commentResult.item(), trackedEvents.size()),
                    commentResult.backoffSeconds());
        }

        Long answerId = newestEvent.postId();
        if (answerId == null) {
            throw new RepositoryPollingException(
                    "StackOverflow answer event does not contain answer id for %s".formatted(question.questionId()));
        }

        StackOverflowItemFetchResult<StackOverflowAnswerResponse> answerResult =
                stackOverflowClient.fetchAnswer(answerId);
        if (answerResult.item() == null) {
            throw new RepositoryPollingException(
                    "Failed to fetch StackOverflow answer %s for %s".formatted(answerId, question.questionId()));
        }

        return new TrackedChangeBuildResult(
                changeBuilder.buildAnswerChange(question, answerResult.item(), trackedEvents.size()),
                answerResult.backoffSeconds());
    }

    private boolean isCommentEvent(StackOverflowQuestionTimelineEventResponse event) {
        return event != null && "comment".equalsIgnoreCase(event.timelineType());
    }

    private record TrackedChangeBuildResult(LinkChange change, Integer backoffSeconds) {}
}
