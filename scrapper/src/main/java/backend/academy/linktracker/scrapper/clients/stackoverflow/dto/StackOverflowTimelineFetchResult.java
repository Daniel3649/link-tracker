package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import java.util.List;

public record StackOverflowTimelineFetchResult(
    List<StackOverflowQuestionTimelineEventResponse> events,
    Integer backoffSeconds
) {
}
