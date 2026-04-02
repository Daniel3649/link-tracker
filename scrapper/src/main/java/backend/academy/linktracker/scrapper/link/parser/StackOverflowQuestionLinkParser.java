package backend.academy.linktracker.scrapper.link.parser;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import java.net.URI;

public class StackOverflowQuestionLinkParser implements LinkParser {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return LinkParser.isValidScheme(uri)
                && ("stackoverflow.com".equalsIgnoreCase(host) || "www.stackoverflow.com".equalsIgnoreCase(host));
    }

    @Override
    public ParsedLink parse(URI uri) {
        if (!LinkParser.isValidScheme(uri)) {
            throw new UnsupportedLinkException("Incorrect StackOverflow link: " + uri);
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new UnsupportedLinkException("Incorrect StackOverflow link: " + uri);
        }

        String[] segments = path.split("/");
        if (segments.length < 3 || !"questions".equals(segments[1])) {
            throw new UnsupportedLinkException("Incorrect StackOverflow link: " + uri);
        }

        long questionId;
        try {
            questionId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            throw new UnsupportedLinkException("Incorrect questionId in link: " + uri, e);
        }

        return new ParsedLink(uri.toString(), new StackOverflowQuestionKey(questionId));
    }
}
