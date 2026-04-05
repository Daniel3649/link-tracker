package backend.academy.linktracker.scrapper.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkChangePreviewFormatterTest {

    private final LinkChangePreviewFormatter formatter = new LinkChangePreviewFormatter();

    @Test
    void shouldNormalizePlainTextPreview() {
        assertThat(formatter.formatPlainText("  Hello \n   world  "))
                .isEqualTo("Hello world");
    }

    @Test
    void shouldStripHtmlAndUnescapeEntities() {
        assertThat(formatter.formatHtml("<p>Hello&nbsp;<b>world</b> &amp; <code>&lt;tag&gt;</code></p>"))
                .isEqualTo("Hello world & <tag>");
    }

    @Test
    void shouldTrimPreviewToTwoHundredCharacters() {
        String input = "a".repeat(210);

        assertThat(formatter.formatPlainText(input))
                .hasSize(200)
                .isEqualTo("a".repeat(200));
    }
}
