package backend.academy.linktracker.scrapper.clients.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryIssueResponse(
        Long id,
        Long number,
        String title,
        String body,
        @JsonProperty("created_at") Instant createdAt,
        GitHubIssueUser user,
        @JsonProperty("pull_request") GitHubPullRequestMarker pullRequest) {
    public boolean isPullRequest() {
        return pullRequest != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubIssueUser(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubPullRequestMarker(String url) {}
}
