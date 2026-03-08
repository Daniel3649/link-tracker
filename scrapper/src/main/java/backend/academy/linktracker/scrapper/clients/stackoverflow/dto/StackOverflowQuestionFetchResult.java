package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

public record StackOverflowQuestionFetchResult(
    StackOverflowQuestionResponse question,
    Integer backoffSeconds
) {
}
