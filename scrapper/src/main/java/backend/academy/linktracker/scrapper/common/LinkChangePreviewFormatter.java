package backend.academy.linktracker.scrapper.common;

import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LinkChangePreviewFormatter {
    private static final int MAX_PREVIEW_LENGTH = 200;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\u00A0]+");

    public String formatPlainText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return normalize(StringEscapeUtils.unescapeHtml4(value));
    }

    public String formatHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String withoutTags = HTML_TAG_PATTERN.matcher(value).replaceAll(" ");
        return normalize(StringEscapeUtils.unescapeHtml4(withoutTags));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = WHITESPACE_PATTERN.matcher(value).replaceAll(" ").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        if (normalized.codePointCount(0, normalized.length()) <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }

        int previewEndIndex = normalized.offsetByCodePoints(0, MAX_PREVIEW_LENGTH);
        return normalized.substring(0, previewEndIndex);
    }
}
