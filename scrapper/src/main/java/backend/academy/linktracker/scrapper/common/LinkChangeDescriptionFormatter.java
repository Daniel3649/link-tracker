package backend.academy.linktracker.scrapper.common;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LinkChangeDescriptionFormatter {
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public String format(LinkChange change) {
        Objects.requireNonNull(change, "change cannot be null");

        if (!change.hasStructuredDetails()) {
            return requireDescription(change);
        }

        List<String> lines = new ArrayList<>();
        lines.add(resolveHeader(change));
        appendIfPresent(lines, "Title", change.title());
        appendIfPresent(lines, "User", change.username());

        if (change.createdAt() != null) {
            lines.add("Created at: " + CREATED_AT_FORMATTER.format(change.createdAt()));
        }

        appendIfPresent(lines, "Preview", change.preview());

        return String.join("\n", lines);
    }

    private String resolveHeader(LinkChange change) {
        if (StringUtils.hasText(change.description())) {
            return change.description();
        }

        return switch (change.type()) {
            case GITHUB_ISSUE -> "New GitHub issue";
            case GITHUB_PULL_REQUEST -> "New GitHub pull request";
            case STACKOVERFLOW_ANSWER -> "New StackOverflow answer";
            case STACKOVERFLOW_COMMENT -> "New StackOverflow comment";
            case RESOURCE_UNAVAILABLE -> "Tracked resource is unavailable";
            case GENERIC -> "Link update";
        };
    }

    private void appendIfPresent(List<String> lines, String label, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(label + ": " + value);
        }
    }

    private String requireDescription(LinkChange change) {
        if (!StringUtils.hasText(change.description())) {
            throw new IllegalArgumentException("Plain LinkChange must contain description");
        }

        return change.description();
    }
}
