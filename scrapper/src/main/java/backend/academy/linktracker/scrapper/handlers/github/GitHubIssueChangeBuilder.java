package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangePreviewFormatter;
import backend.academy.linktracker.scrapper.common.LinkChangeSource;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
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
        String description = extraUpdatesCount > 0
                ? "%s (+%d more updates)".formatted(resolveHeader(issue), extraUpdatesCount)
                : resolveHeader(issue);

        if (issue.isPullRequest()) {
            return new LinkChange(
                    description,
                    LinkChangeSource.GITHUB,
                    LinkChangeType.GITHUB_PULL_REQUEST,
                    issue.title(),
                    issue.user() == null ? null : issue.user().login(),
                    issue.createdAt(),
                    previewFormatter.formatPlainText(issue.body()));
        }

        return new LinkChange(
                description,
                LinkChangeSource.GITHUB,
                LinkChangeType.GITHUB_ISSUE,
                issue.title(),
                issue.user() == null ? null : issue.user().login(),
                issue.createdAt(),
                previewFormatter.formatPlainText(issue.body()));
    }

    private String resolveHeader(GitHubRepositoryIssueResponse issue) {
        return issue.isPullRequest() ? "New GitHub pull request" : "New GitHub issue";
    }
}
