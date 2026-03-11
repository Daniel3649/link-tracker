package backend.academy.linktracker.contract.link.parser;

import backend.academy.linktracker.contract.link.dto.ParsedSupportedLink;
import backend.academy.linktracker.contract.link.exception.UnsupportedLinkFormatException;
import lombok.RequiredArgsConstructor;
import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
public class SupportedLinkParser {
    private final List<LinkParser<? extends ParsedSupportedLink>> parsers;

    public ParsedSupportedLink parse(String rawLink) {
        if (rawLink == null || rawLink.isBlank()) {
            throw new UnsupportedLinkFormatException("Link must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(rawLink.strip());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedLinkFormatException("Incorrect link: " + rawLink, e);
        }

        return parse(uri);
    }

    public ParsedSupportedLink parse(URI uri) {
        if (uri == null) {
            throw new UnsupportedLinkFormatException("Link must not be null");
        }

        validateScheme(uri);

        for (LinkParser<? extends ParsedSupportedLink> parser : parsers) {
            if (parser.supports(uri)) {
                return parser.parse(uri);
            }
        }

        throw new UnsupportedLinkFormatException(
            "Supported links are only GitHub repository and StackOverflow question"
        );
    }

    public boolean supports(URI uri) {
        if (uri == null) {
            return false;
        }

        try {
            validateScheme(uri);
        } catch (UnsupportedLinkFormatException e) {
            return false;
        }

        return parsers.stream().anyMatch(parser -> parser.supports(uri));
    }

    private void validateScheme(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
            || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new UnsupportedLinkFormatException(
                "Link must start with http:// or https://"
            );
        }
    }
}
