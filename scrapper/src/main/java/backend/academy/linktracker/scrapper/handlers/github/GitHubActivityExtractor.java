package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryActivityResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class GitHubActivityExtractor {
    public List<GitHubRepositoryActivityResponse> extractNewActivities(
            List<GitHubRepositoryActivityResponse> recentActivities, Long lastSeenActivityId) {
        if (recentActivities == null || recentActivities.isEmpty()) {
            return List.of();
        }

        if (lastSeenActivityId == null) {
            return recentActivities;
        }

        List<GitHubRepositoryActivityResponse> result = new ArrayList<>();
        for (GitHubRepositoryActivityResponse activity : recentActivities) {
            if (Objects.equals(activity.id(), lastSeenActivityId)) {
                break;
            }
            result.add(activity);
        }
        return result;
    }
}
