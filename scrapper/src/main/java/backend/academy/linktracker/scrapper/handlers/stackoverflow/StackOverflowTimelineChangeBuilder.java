package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeSource;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StackOverflowTimelineChangeBuilder {
    public LinkChange buildChange(List<StackOverflowQuestionTimelineEventResponse> newEvents) {
        int count = newEvents == null ? 0 : newEvents.size();

        if (count <= 0) {
            return LinkChange.plain("Question changed");
        }

        if (count == 1) {
            return toSingleChange(newEvents.getFirst());
        }

        long answers = newEvents.stream().filter(this::isAnswer).count();
        long comments = count - answers;

        if (comments == 0) {
            return LinkChange.plain("Question has %d new answers".formatted(count));
        }

        if (answers == 0) {
            return LinkChange.plain("Question has %d new comments".formatted(count));
        }

        return LinkChange.plain("Question has %d new answers or comments".formatted(count));
    }

    private LinkChange toSingleChange(StackOverflowQuestionTimelineEventResponse event) {
        if (isComment(event)) {
            return new LinkChange(
                    "New StackOverflow comment",
                    LinkChangeSource.STACKOVERFLOW,
                    LinkChangeType.STACKOVERFLOW_COMMENT,
                    null,
                    null,
                    null,
                    null);
        }

        return new LinkChange(
                "New StackOverflow answer",
                LinkChangeSource.STACKOVERFLOW,
                LinkChangeType.STACKOVERFLOW_ANSWER,
                null,
                null,
                null,
                null);
    }

    private boolean isAnswer(StackOverflowQuestionTimelineEventResponse event) {
        return event != null && "answer".equalsIgnoreCase(event.timelineType());
    }

    private boolean isComment(StackOverflowQuestionTimelineEventResponse event) {
        return event != null && "comment".equalsIgnoreCase(event.timelineType());
    }
}
