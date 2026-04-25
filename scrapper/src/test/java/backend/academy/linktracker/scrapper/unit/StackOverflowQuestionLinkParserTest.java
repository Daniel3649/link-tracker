package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.link.parser.StackOverflowQuestionLinkParser;
import java.net.URI;
import org.junit.jupiter.api.Test;

class StackOverflowQuestionLinkParserTest {
    private final StackOverflowQuestionLinkParser parser = new StackOverflowQuestionLinkParser();

    @Test
    void shouldParseQuestionLink() {
        URI uri = URI.create("https://stackoverflow.com/questions/123456/example");

        ParsedLink parsed = parser.parse(uri);

        assertThat(parsed.url()).isEqualTo("https://stackoverflow.com/questions/123456/example");
        assertThat(parsed.resourceKey()).isEqualTo(new StackOverflowQuestionKey(123456L));
    }

    @Test
    void shouldRejectUnsupportedStackOverflowLink() {
        URI uri = URI.create("https://stackoverflow.com/users/123456/example");

        assertThatThrownBy(() -> parser.parse(uri))
                .isInstanceOf(UnsupportedLinkException.class)
                .hasMessageContaining("Incorrect StackOverflow link");
    }
}
