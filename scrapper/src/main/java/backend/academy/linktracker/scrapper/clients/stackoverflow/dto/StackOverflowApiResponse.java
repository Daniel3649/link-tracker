package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowApiResponse<T>(
    List<T> items,
    Integer backoff,

    @JsonProperty("quota_remaining")
    Integer quotaRemaining,

    @JsonProperty("error_id")
    Integer errorId,

    @JsonProperty("error_message")
    String errorMessage,

    @JsonProperty("error_name")
    String errorName
) {
}
