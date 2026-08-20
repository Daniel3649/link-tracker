package backend.academy.linktracker.scrapper.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LinkChangeDescriptionFormatterTest {
    @Test
    void shouldKeepPlainDescriptionUntouched() {
        LinkChange change = LinkChange.plain("Repository changed");

        String description = LinkChangeDescriptionFormatter.format(change);

        assertThat(description).isEqualTo("Repository changed");
    }

    @Test
    void shouldBuildDescriptionFromStructuredFieldsWithoutFallback() {
        LinkChange change = new LinkChange(
                null,
                LinkChangeSource.GITHUB,
                LinkChangeType.GITHUB_PULL_REQUEST,
                "Add notifications",
                "octocat",
                Instant.parse("2026-04-05T09:30:00Z"),
                "First 200 chars");

        String description = LinkChangeDescriptionFormatter.format(change);

        assertThat(description).isEqualTo("""
                        New GitHub pull request
                        Title: Add notifications
                        User: octocat
                        Created at: 2026-04-05T09:30:00Z
                        Preview: First 200 chars""");
    }
}
