package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.clients.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowTimelineFetchResult;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.handlers.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.common.ParsedLink;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StackOverflowLinkParser implements LinkHandler {
    private static final int TIMELINE_FETCH_LIMIT = 10;
    private static final long MIN_IDENTICAL_REQUEST_INTERVAL_SECONDS = 60L;

    private final StackOverflowTrackingStateRepository repository;
    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return "stackoverflow.com".equalsIgnoreCase(host) || "www.stackoverflow.com".equalsIgnoreCase(host);
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] segments = uri.getPath().split("/");

        if (segments.length < 3 || !"questions".equals(segments[1])) {
            throw new UnsupportedLinkException("Incorrect StackOverflow link: " + uri);
        }

        long questionId;
        try {
            questionId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            throw new UnsupportedLinkException("Incorrect questionId in link: " + uri, e);
        }

        return new ParsedLink(uri.toString(), new StackOverflowQuestionKey(questionId));
    }

    public void createTrackingState(TrackedLink trackedLink) {
        StackOverflowQuestionKey key = extractKey(trackedLink.getResourceKey());

        StackOverflowQuestionFetchResult questionResult = stackOverflowClient.fetchQuestion(key);
        if (questionResult.question() == null) {
            throw new RepositoryPollingException(
                    "Failed to initialize StackOverflow tracking state for %s: question not found"
                            .formatted(trackedLink.getUrl()));
        }

        StackOverflowTimelineFetchResult timelineResult = stackOverflowClient.fetchQuestionTimeline(key, 1);

        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setTimelineCursor(buildInitialCursor(questionResult.question(), timelineResult.events()));
        state.setNextCheckAt(calculateNextCheckAt(questionResult.backoffSeconds(), timelineResult.backoffSeconds()));

        boolean saved = repository.saveIfAbsent(state);
        if (!saved) {
            throw new TrackingStateAlreadyExistsException(
                    "Tracking state already exists for link: " + trackedLink.getUrl());
        }
    }

    @Override
    public void deleteTrackingState(TrackedLink trackedLink) {
        repository.deleteByTrackedLink(trackedLink);
    }

    @Override
    public Optional<LinkChange> checkForUpdate(TrackedLink trackedLink) {
        Optional<StackOverflowTrackingState> optionalState = repository.findByTrackedLink(trackedLink);
        if (optionalState.isEmpty()) {
            return Optional.empty();
        }

        StackOverflowTrackingState state = optionalState.get();

        if (state.getNextCheckAt() != null && Instant.now().isBefore(state.getNextCheckAt())) {
            return Optional.empty();
        }

        StackOverflowQuestionKey key = extractKey(trackedLink.getResourceKey());
        StackOverflowTimelineCursor cursor =
                state.getTimelineCursor() != null ? state.getTimelineCursor() : StackOverflowTimelineCursor.empty();

        StackOverflowQuestionFetchResult questionResult = stackOverflowClient.fetchQuestion(key);
        StackOverflowQuestionResponse question = questionResult.question();

        if (question == null) {
            state.setNextCheckAt(calculateNextCheckAt(questionResult.backoffSeconds(), null));
            repository.save(state);
            throw new RepositoryPollingException("Question is unavailable: " + key.questionId());
        }

        long currentLastActivity = safeLong(question.lastActivityDateEpochSec());
        if (currentLastActivity <= cursor.lastQuestionActivityDateEpochSec()) {
            state.setNextCheckAt(calculateNextCheckAt(questionResult.backoffSeconds(), null));
            repository.save(state);
            return Optional.empty();
        }

        StackOverflowTimelineFetchResult timelineResult =
                stackOverflowClient.fetchQuestionTimeline(key, TIMELINE_FETCH_LIMIT);

        List<StackOverflowQuestionTimelineEventResponse> newEvents = extractNewEvents(timelineResult.events(), cursor);

        state.setTimelineCursor(buildUpdatedCursor(question, timelineResult.events(), cursor));
        state.setNextCheckAt(calculateNextCheckAt(questionResult.backoffSeconds(), timelineResult.backoffSeconds()));
        repository.save(state);

        if (newEvents.isEmpty()) {
            return Optional.of(new LinkChange("Question changed: " + question.title()));
        }

        return Optional.of(new LinkChange(buildDescription(newEvents)));
    }

    private List<StackOverflowQuestionTimelineEventResponse> extractNewEvents(
            List<StackOverflowQuestionTimelineEventResponse> events, StackOverflowTimelineCursor cursor) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        if (!StringUtils.hasText(cursor.lastEventKey())) {
            return events;
        }

        List<StackOverflowQuestionTimelineEventResponse> result = new ArrayList<>();
        for (StackOverflowQuestionTimelineEventResponse event : events) {
            String eventKey = buildEventKey(event);

            if (Objects.equals(eventKey, cursor.lastEventKey())) {
                break;
            }

            if (safeLong(event.creationDateEpochSec()) < cursor.lastCreationDateEpochSec()) {
                break;
            }

            result.add(event);
        }

        return result;
    }

    private StackOverflowTimelineCursor buildUpdatedCursor(
            StackOverflowQuestionResponse question,
            List<StackOverflowQuestionTimelineEventResponse> events,
            StackOverflowTimelineCursor oldCursor) {
        if (events == null || events.isEmpty()) {
            return new StackOverflowTimelineCursor(
                    safeLong(question.lastActivityDateEpochSec()),
                    oldCursor.lastCreationDateEpochSec(),
                    oldCursor.lastEventKey());
        }

        StackOverflowQuestionTimelineEventResponse newest = events.getFirst();
        return new StackOverflowTimelineCursor(
                safeLong(question.lastActivityDateEpochSec()),
                safeLong(newest.creationDateEpochSec()),
                buildEventKey(newest));
    }

    private StackOverflowTimelineCursor buildInitialCursor(
            StackOverflowQuestionResponse question, List<StackOverflowQuestionTimelineEventResponse> events) {
        if (events == null || events.isEmpty()) {
            return new StackOverflowTimelineCursor(safeLong(question.lastActivityDateEpochSec()), 0L, null);
        }

        StackOverflowQuestionTimelineEventResponse newest = events.getFirst();
        return new StackOverflowTimelineCursor(
                safeLong(question.lastActivityDateEpochSec()),
                safeLong(newest.creationDateEpochSec()),
                buildEventKey(newest));
    }

    private String buildDescription(List<StackOverflowQuestionTimelineEventResponse> newEvents) {

        if (newEvents.size() == 1) {
            return "Question changed: one event happened";
        }

        return "Question changed: %d events happened)".formatted(newEvents.size() - 1);
    }

    private StackOverflowQuestionKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof StackOverflowQuestionKey key)) {
            throw new IllegalArgumentException("Resource key is not StackOverflowQuestionKey");
        }
        return key;
    }

    private Instant calculateNextCheckAt(Integer... backoffValues) {
        long waitSeconds = MIN_IDENTICAL_REQUEST_INTERVAL_SECONDS;

        if (backoffValues != null) {
            for (Integer backoff : backoffValues) {
                if (backoff != null) {
                    waitSeconds = Math.max(waitSeconds, backoff.longValue());
                }
            }
        }

        return Instant.now().plusSeconds(waitSeconds);
    }

    private String buildEventKey(StackOverflowQuestionTimelineEventResponse event) {
        return "%s:%s:%s:%s:%s:%s"
                .formatted(
                        safeString(event.timelineType()),
                        safeLong(event.creationDateEpochSec()),
                        safeLong(event.questionId()),
                        safeLong(event.postId()),
                        safeLong(event.commentId()),
                        safeString(event.revisionGuid()));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
