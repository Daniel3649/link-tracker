package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowTimelineFetchResult;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.link.parser.StackOverflowQuestionLinkParser;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
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
    private final StackOverflowTimelineDescriptionBuilder descriptionBuilder;
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
        state.setTimelineCursor(timelineSupport.buildInitialCursor(timelineResult.events()));
        state.setNextCheckAt(
                timelineSupport.calculateNextCheckAt(questionResult.backoffSeconds(), timelineResult.backoffSeconds()));
        state.setLastQuestionActivityDateEpochSec(questionResult.question().lastActivityDateEpochSec());

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

            return Optional.of(new LinkChange("Question is unavailable: " + key.questionId()));
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

        state.setTimelineCursor(timelineSupport.buildUpdatedCursor(timelineResult.events(), cursor));
        state.setNextCheckAt(
                timelineSupport.calculateNextCheckAt(questionResult.backoffSeconds(), timelineResult.backoffSeconds()));
        state.setLastQuestionActivityDateEpochSec(timelineSupport.safeLong(currentLastActivity));

        repository.save(state);

        if (newEvents.isEmpty()) {
            return Optional.of(new LinkChange("Something changed"));
        }

        return Optional.of(new LinkChange(descriptionBuilder.buildDescription(newEvents)));
    }

    private StackOverflowQuestionKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof StackOverflowQuestionKey key)) {
            throw new IllegalArgumentException("Resource key is not StackOverflowQuestionKey");
        }
        return key;
    }
}
