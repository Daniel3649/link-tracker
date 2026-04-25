package backend.academy.linktracker.scrapper.common;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowOwnerResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@EqualsAndHashCode
@ToString
public final class LinkChange {
    private final String description;
    private final LinkChangeSource source;
    private final LinkChangeType type;
    private final String title;
    private final String username;
    private final Instant createdAt;
    private final String preview;

    public LinkChange(
            String description,
            LinkChangeSource source,
            LinkChangeType type,
            String title,
            String username,
            Instant createdAt,
            String preview) {
        this.description = normalize(description);
        this.source = source == null ? LinkChangeSource.UNKNOWN : source;
        this.type = type == null ? LinkChangeType.GENERIC : type;
        this.title = normalize(title);
        this.username = normalize(username);
        this.createdAt = createdAt;
        this.preview = normalize(preview);

        if (!StringUtils.hasText(this.description) && !hasStructuredDetails(this.title, this.username, this.createdAt, this.preview)) {
            throw new IllegalArgumentException("LinkChange requires fallback description or structured details");
        }
    }

    public LinkChange(GitHubRepositoryIssueResponse issue, int extraUpdatesCount) {
        this(
                resolveGitHubDescription(issue, extraUpdatesCount),
                LinkChangeSource.GITHUB,
                resolveGitHubType(issue),
                issue == null ? null : issue.title(),
                issue == null || issue.user() == null ? null : issue.user().login(),
                issue == null ? null : issue.createdAt(),
                LinkChangePreviewFormatter.formatPlainText(issue == null ? null : issue.body()));
    }

    public LinkChange(StackOverflowQuestionResponse question, StackOverflowAnswerResponse answer, int trackedEventsCount) {
        this(
                resolveStackOverflowDescription("New StackOverflow answer", trackedEventsCount),
                LinkChangeSource.STACKOVERFLOW,
                LinkChangeType.STACKOVERFLOW_ANSWER,
                question == null ? null : question.title(),
                extractUsername(answer == null ? null : answer.owner()),
                toInstant(answer == null ? null : answer.creationDateEpochSec()),
                LinkChangePreviewFormatter.formatHtml(answer == null ? null : answer.body()));
    }

    public LinkChange(StackOverflowQuestionResponse question, StackOverflowCommentResponse comment, int trackedEventsCount) {
        this(
                resolveStackOverflowDescription("New StackOverflow comment", trackedEventsCount),
                LinkChangeSource.STACKOVERFLOW,
                LinkChangeType.STACKOVERFLOW_COMMENT,
                question == null ? null : question.title(),
                extractUsername(comment == null ? null : comment.owner()),
                toInstant(comment == null ? null : comment.creationDateEpochSec()),
                LinkChangePreviewFormatter.formatHtml(comment == null ? null : comment.body()));
    }

    public LinkChange(String description) {
        this(description, LinkChangeSource.UNKNOWN, LinkChangeType.GENERIC, null, null, null, null);
    }

    public static LinkChange plain(String description) {
        return new LinkChange(description);
    }

    public boolean hasStructuredDetails() {
        return hasStructuredDetails(title, username, createdAt, preview);
    }

    private static String resolveGitHubDescription(GitHubRepositoryIssueResponse issue, int extraUpdatesCount) {
        String header = issue != null && issue.isPullRequest() ? "New GitHub pull request" : "New GitHub issue";
        if (extraUpdatesCount <= 0) {
            return header;
        }

        return "%s (+%d more updates)".formatted(header, extraUpdatesCount);
    }

    private static LinkChangeType resolveGitHubType(GitHubRepositoryIssueResponse issue) {
        return issue != null && issue.isPullRequest()
                ? LinkChangeType.GITHUB_PULL_REQUEST
                : LinkChangeType.GITHUB_ISSUE;
    }

    private static String resolveStackOverflowDescription(String baseDescription, int trackedEventsCount) {
        if (trackedEventsCount <= 1) {
            return baseDescription;
        }

        return "%s (+%d more updates)".formatted(baseDescription, trackedEventsCount - 1);
    }

    private static String extractUsername(StackOverflowOwnerResponse owner) {
        return owner == null ? null : owner.displayName();
    }

    private static Instant toInstant(Long epochSec) {
        return epochSec == null ? null : Instant.ofEpochSecond(epochSec);
    }

    private static boolean hasStructuredDetails(String title, String username, Instant createdAt, String preview) {
        return StringUtils.hasText(title)
                || StringUtils.hasText(username)
                || createdAt != null
                || StringUtils.hasText(preview);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
