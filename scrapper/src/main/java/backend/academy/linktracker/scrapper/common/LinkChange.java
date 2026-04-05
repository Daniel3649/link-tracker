package backend.academy.linktracker.scrapper.common;

import java.time.Instant;
import org.springframework.util.StringUtils;

public record LinkChange(
        String description,
        LinkChangeSource source,
        LinkChangeType type,
        String title,
        String username,
        Instant createdAt,
        String preview) {
    public LinkChange {
        description = normalize(description);
        source = source == null ? LinkChangeSource.UNKNOWN : source;
        type = type == null ? LinkChangeType.GENERIC : type;
        title = normalize(title);
        username = normalize(username);
        preview = normalize(preview);

        if (!StringUtils.hasText(description) && !hasStructuredDetails(title, username, createdAt, preview)) {
            throw new IllegalArgumentException("LinkChange requires fallback description or structured details");
        }
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
