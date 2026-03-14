package backend.academy.linktracker.contract.link.common;

import java.net.URI;
import java.util.Objects;

public record ParsedStackOverflowQuestionLink(
    URI uri,
    long questionId
) implements ParsedSupportedLink {

    public ParsedStackOverflowQuestionLink {
        Objects.requireNonNull(uri);

        if (questionId <= 0) {
            throw new IllegalArgumentException("questionId must be positive");
        }
    }
}
