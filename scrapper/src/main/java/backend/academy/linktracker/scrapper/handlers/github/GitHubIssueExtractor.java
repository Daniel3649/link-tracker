package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class GitHubIssueExtractor {
    public List<GitHubRepositoryIssueResponse> extractNewIssuesOrPullRequests(
            List<GitHubRepositoryIssueResponse> recentIssues, Long lastSeenIssueId) {
        if (recentIssues == null || recentIssues.isEmpty()) {
            return List.of();
        }

        if (lastSeenIssueId == null) {
            return recentIssues;
        }

        List<GitHubRepositoryIssueResponse> result = new ArrayList<>();
        for (GitHubRepositoryIssueResponse issue : recentIssues) {
            if (Objects.equals(issue.id(), lastSeenIssueId)) {
                break;
            }
            result.add(issue);
        }

        return result;
    }
}
