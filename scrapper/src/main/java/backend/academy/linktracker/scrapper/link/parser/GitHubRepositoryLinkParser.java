package backend.academy.linktracker.scrapper.link.parser;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import java.net.URI;

public class GitHubRepositoryLinkParser implements LinkParser {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return LinkParser.isValidScheme(uri)
                && ("github.com".equalsIgnoreCase(host) || "www.github.com".equalsIgnoreCase(host));
    }

    @Override
    public ParsedLink parse(URI uri) {
        if (!LinkParser.isValidScheme(uri)) {
            throw new UnsupportedLinkException("Incorrect GitHub link: " + uri);
        }

        String path = uri.getPath();
        if (hasNotText(path)) {
            throw new UnsupportedLinkException("Incorrect GitHub link: " + uri);
        }

        String[] segments = path.split("/");
        if (segments.length < 3 || hasNotText(segments[1]) || hasNotText(segments[2])) {
            throw new UnsupportedLinkException("Incorrect GitHub link: " + uri);
        }

        return new ParsedLink(uri.toString(), new GitHubRepositoryKey(segments[1], segments[2]));
    }

    private boolean hasNotText(String raw) {
        return raw == null || raw.isBlank();
    }
}
