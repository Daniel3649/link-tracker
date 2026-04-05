package backend.academy.linktracker.scrapper.handlers.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeSource;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GitHubIssueChangeBuilder {
    public LinkChange buildChange(GitHubRepositoryKey key, List<GitHubRepositoryIssueResponse> newIssues) {
        int count = newIssues == null ? 0 : newIssues.size();

        if (count <= 0) {
            return LinkChange.plain("Repository changed: " + key.owner() + "/" + key.repo());
        }

        if (count == 1) {
            return toSingleChange(newIssues.getFirst());
        }

        long pullRequests = newIssues.stream().filter(GitHubRepositoryIssueResponse::isPullRequest).count();
        long issues = count - pullRequests;

        if (pullRequests == 0) {
            return LinkChange.plain(
                    "Repository %s/%s has %d new issues".formatted(key.owner(), key.repo(), count));
        }

        if (issues == 0) {
            return LinkChange.plain(
                    "Repository %s/%s has %d new pull requests".formatted(key.owner(), key.repo(), count));
        }

        return LinkChange.plain(
                "Repository %s/%s has %d new issues or pull requests".formatted(key.owner(), key.repo(), count));
    }

    private LinkChange toSingleChange(GitHubRepositoryIssueResponse issue) {
        if (issue.isPullRequest()) {
            return new LinkChange(
                    "New GitHub pull request",
                    LinkChangeSource.GITHUB,
                    LinkChangeType.GITHUB_PULL_REQUEST,
                    null,
                    null,
                    null,
                    null);
        }

        return new LinkChange(
                "New GitHub issue",
                LinkChangeSource.GITHUB,
                LinkChangeType.GITHUB_ISSUE,
                null,
                null,
                null,
                null);
    }
}
