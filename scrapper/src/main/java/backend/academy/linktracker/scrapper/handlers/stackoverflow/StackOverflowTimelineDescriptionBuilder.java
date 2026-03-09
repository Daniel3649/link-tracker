package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class StackOverflowTimelineDescriptionBuilder {
    public String buildDescription(List<StackOverflowQuestionTimelineEventResponse> newEvents) {
        int count = newEvents == null ? 0 : newEvents.size();

        if (count <= 0) {
            return "Question changed";
        }

        if (count == 1) {
            return "Question changed: one event happened";
        }

        return "Question changed: %d events happened".formatted(newEvents.size());
    }
}
