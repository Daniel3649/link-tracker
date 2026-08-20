package backend.academy.linktracker.scrapper.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkChangePreviewFormatterTest {
    @Test
    void shouldNormalizePlainTextPreview() {
        assertThat(LinkChangePreviewFormatter.formatPlainText("  Hello \n   world  "))
                .isEqualTo("Hello world");
    }

    @Test
    void shouldStripHtmlAndUnescapeEntities() {
        assertThat(LinkChangePreviewFormatter.formatHtml(
                        "<p>Hello&nbsp;<b>world</b> &amp; <code>&lt;tag&gt;</code></p>"))
                .isEqualTo("Hello world & <tag>");
    }

    @Test
    void shouldTrimPreviewToTwoHundredCharacters() {
        String input = "a".repeat(210);

        assertThat(LinkChangePreviewFormatter.formatPlainText(input))
                .hasSize(200)
                .isEqualTo("a".repeat(200));
    }
}
