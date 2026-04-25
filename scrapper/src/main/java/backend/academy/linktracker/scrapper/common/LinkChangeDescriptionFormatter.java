package backend.academy.linktracker.scrapper.common;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

public final class LinkChangeDescriptionFormatter {
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private LinkChangeDescriptionFormatter() {}

    public static String format(LinkChange change) {
        Objects.requireNonNull(change, "change cannot be null");

        if (!change.hasStructuredDetails()) {
            return requireDescription(change);
        }

        List<String> lines = new ArrayList<>();
        lines.add(resolveHeader(change));
        appendIfPresent(lines, "Title", change.getTitle());
        appendIfPresent(lines, "User", change.getUsername());

        if (change.getCreatedAt() != null) {
            lines.add("Created at: " + CREATED_AT_FORMATTER.format(change.getCreatedAt()));
        }

        appendIfPresent(lines, "Preview", change.getPreview());

        return String.join("\n", lines);
    }

    private static String resolveHeader(LinkChange change) {
        if (StringUtils.hasText(change.getDescription())) {
            return change.getDescription();
        }

        return switch (change.getType()) {
            case GITHUB_ISSUE -> "New GitHub issue";
            case GITHUB_PULL_REQUEST -> "New GitHub pull request";
            case STACKOVERFLOW_ANSWER -> "New StackOverflow answer";
            case STACKOVERFLOW_COMMENT -> "New StackOverflow comment";
            case RESOURCE_UNAVAILABLE -> "Tracked resource is unavailable";
            case GENERIC -> "Link update";
        };
    }

    private static void appendIfPresent(List<String> lines, String label, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(label + ": " + value);
        }
    }

    private static String requireDescription(LinkChange change) {
        if (!StringUtils.hasText(change.getDescription())) {
            throw new IllegalArgumentException("Plain LinkChange must contain description");
        }

        return change.getDescription();
    }
}
