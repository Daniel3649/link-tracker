package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import java.net.URI;
import org.junit.jupiter.api.Test;

class GitHubRepositoryLinkParserTest {
    private final GitHubRepositoryLinkParser parser = new GitHubRepositoryLinkParser();

    @Test
    void shouldParseRepositoryLink() {
        URI uri = URI.create("https://github.com/octocat/Hello-World");

        ParsedLink parsed = parser.parse(uri);

        assertThat(parsed.url()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(parsed.resourceKey()).isEqualTo(new GitHubRepositoryKey("octocat", "Hello-World"));
    }

    @Test
    void shouldRejectUnsupportedGitHubLink() {
        URI uri = URI.create("https://github.com/octocat");

        assertThatThrownBy(() -> parser.parse(uri))
                .isInstanceOf(UnsupportedLinkException.class)
                .hasMessageContaining("Incorrect GitHub link");
    }
}
