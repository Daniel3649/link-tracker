package backend.academy.linktracker.scrapper.handlers.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangePreviewFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackOverflowTimelineChangeBuilder {
    private final LinkChangePreviewFormatter previewFormatter;

    public LinkChange buildAnswerChange(
            StackOverflowQuestionResponse question, StackOverflowAnswerResponse answer, int trackedEventsCount) {
        return new LinkChange(question, answer, trackedEventsCount, previewFormatter);
    }

    public LinkChange buildCommentChange(
            StackOverflowQuestionResponse question, StackOverflowCommentResponse comment, int trackedEventsCount) {
        return new LinkChange(question, comment, trackedEventsCount, previewFormatter);
    }
}
