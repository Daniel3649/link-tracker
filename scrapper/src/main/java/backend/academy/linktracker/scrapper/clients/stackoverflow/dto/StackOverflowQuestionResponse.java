package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowQuestionResponse(
    @JsonProperty("question_id")
    Long questionId,

    @JsonProperty("last_activity_date")
    Long lastActivityDateEpochSec,

    String title
) {
}
