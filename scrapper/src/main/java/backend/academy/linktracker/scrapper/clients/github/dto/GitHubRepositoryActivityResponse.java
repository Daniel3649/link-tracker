package backend.academy.linktracker.scrapper.clients.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryActivityResponse(
    Long id,

    @JsonProperty("activity_type")
    String activityType,

    String ref,

    @JsonProperty("before")
    String beforeSha,

    @JsonProperty("after")
    String afterSha,

    @JsonProperty("pushed_at")
    Instant pushedAt,

    @JsonProperty("push_type")
    String pushType,

    GitHubActivityActor pusher
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubActivityActor(String login) {
    }
}
