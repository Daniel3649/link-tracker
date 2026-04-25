package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangePreviewFormatter;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubIssueChangeBuilder {
    private final LinkChangePreviewFormatter previewFormatter;

    public LinkChange buildChange(GitHubRepositoryKey key, List<GitHubRepositoryIssueResponse> newIssues) {
        int count = newIssues == null ? 0 : newIssues.size();

        if (count == 0) {
            return LinkChange.plain("Repository changed: " + key.owner() + "/" + key.repo());
        }

        return toStructuredChange(newIssues.getFirst(), count - 1);
    }

    private LinkChange toStructuredChange(GitHubRepositoryIssueResponse issue, int extraUpdatesCount) {
        return new LinkChange(issue, extraUpdatesCount, previewFormatter);
    }
}
