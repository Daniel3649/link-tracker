package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowOwnerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangePreviewFormatter;
import backend.academy.linktracker.scrapper.common.LinkChangeSource;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackOverflowTimelineChangeBuilder {
    private final LinkChangePreviewFormatter previewFormatter;

    public LinkChange buildAnswerChange(
            StackOverflowQuestionResponse question, StackOverflowAnswerResponse answer, int trackedEventsCount) {
        return new LinkChange(
                resolveDescription("New StackOverflow answer", trackedEventsCount),
                LinkChangeSource.STACKOVERFLOW,
                LinkChangeType.STACKOVERFLOW_ANSWER,
                question == null ? null : question.title(),
                extractUsername(answer == null ? null : answer.owner()),
                toInstant(answer == null ? null : answer.creationDateEpochSec()),
                previewFormatter.formatHtml(answer == null ? null : answer.body()));
    }

    public LinkChange buildCommentChange(
            StackOverflowQuestionResponse question, StackOverflowCommentResponse comment, int trackedEventsCount) {
        return new LinkChange(
                resolveDescription("New StackOverflow comment", trackedEventsCount),
                LinkChangeSource.STACKOVERFLOW,
                LinkChangeType.STACKOVERFLOW_COMMENT,
                question == null ? null : question.title(),
                extractUsername(comment == null ? null : comment.owner()),
                toInstant(comment == null ? null : comment.creationDateEpochSec()),
                previewFormatter.formatHtml(comment == null ? null : comment.body()));
    }

    private String resolveDescription(String baseDescription, int trackedEventsCount) {
        if (trackedEventsCount <= 1) {
            return baseDescription;
        }

        return "%s (+%d more updates)".formatted(baseDescription, trackedEventsCount - 1);
    }

    private String extractUsername(StackOverflowOwnerResponse owner) {
        return owner == null ? null : owner.displayName();
    }

    private Instant toInstant(Long epochSec) {
        return epochSec == null ? null : Instant.ofEpochSecond(epochSec);
    }
}
