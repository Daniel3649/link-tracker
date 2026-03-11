package backend.academy.linktracker.contract.link.parser;

import backend.academy.linktracker.contract.link.dto.ParsedStackOverflowQuestionLink;
import backend.academy.linktracker.contract.link.exception.UnsupportedLinkFormatException;
import java.net.URI;

public class StackOverflowQuestionLinkParser implements LinkParser<ParsedStackOverflowQuestionLink> {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return "stackoverflow.com".equalsIgnoreCase(host)
            || "www.stackoverflow.com".equalsIgnoreCase(host);
    }

    @Override
    public ParsedStackOverflowQuestionLink parse(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new UnsupportedLinkFormatException("Incorrect StackOverflow link: " + uri);
        }

        String[] segments = path.split("/");
        if (segments.length < 3 || !"questions".equals(segments[1])) {
            throw new UnsupportedLinkFormatException("Incorrect StackOverflow link: " + uri);
        }

        long questionId;
        try {
            questionId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            throw new UnsupportedLinkFormatException("Incorrect questionId in link: " + uri, e);
        }

        return new ParsedStackOverflowQuestionLink(uri, questionId);
    }


}
