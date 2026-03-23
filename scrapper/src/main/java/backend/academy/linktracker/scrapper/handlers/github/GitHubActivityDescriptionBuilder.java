package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryActivityResponse;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GitHubActivityDescriptionBuilder {
    public String buildDescription(GitHubRepositoryKey key, List<GitHubRepositoryActivityResponse> newActivities) {
        int count = newActivities == null ? 0 : newActivities.size();

        if (count == 0) {
            return "Repository changed: " + key.owner() + "/" + key.repo();
        }

        if (count == 1) {
            return "Repository %s/%s changed: one event happened".formatted(key.owner(), key.repo());
        }

        return "Repository %s/%s changed: %d events happened".formatted(key.owner(), key.repo(), count);
    }
}
