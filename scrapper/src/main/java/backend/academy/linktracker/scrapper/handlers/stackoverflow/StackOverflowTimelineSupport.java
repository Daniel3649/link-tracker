package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StackOverflowTimelineSupport {
    private static final long MIN_IDENTICAL_REQUEST_INTERVAL_SECONDS = 60L;

    public StackOverflowTimelineCursor buildInitialCursor(List<StackOverflowQuestionTimelineEventResponse> events) {
        if (events == null || events.isEmpty()) {
            return new StackOverflowTimelineCursor(0L, null);
        }

        StackOverflowQuestionTimelineEventResponse newest = events.getFirst();
        return new StackOverflowTimelineCursor(safeLong(newest.creationDateEpochSec()), buildEventKey(newest));
    }

    public StackOverflowTimelineCursor buildUpdatedCursor(
            List<StackOverflowQuestionTimelineEventResponse> events, StackOverflowTimelineCursor oldCursor) {
        if (events == null || events.isEmpty()) {
            return oldCursor;
        }

        StackOverflowQuestionTimelineEventResponse newest = events.getFirst();
        return new StackOverflowTimelineCursor(safeLong(newest.creationDateEpochSec()), buildEventKey(newest));
    }

    public List<StackOverflowQuestionTimelineEventResponse> extractNewEvents(
            List<StackOverflowQuestionTimelineEventResponse> events, StackOverflowTimelineCursor cursor) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        if (cursor == null || !StringUtils.hasText(cursor.lastEventKey())) {
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

    public Instant calculateNextCheckAt(Integer... backoffValues) {
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

    public String buildEventKey(StackOverflowQuestionTimelineEventResponse event) {
        return "%s:%s:%s:%s:%s:%s"
                .formatted(
                        safeString(event.timelineType()),
                        safeLong(event.creationDateEpochSec()),
                        safeLong(event.questionId()),
                        safeLong(event.postId()),
                        safeLong(event.commentId()),
                        safeString(event.revisionGuid()));
    }

    public long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
