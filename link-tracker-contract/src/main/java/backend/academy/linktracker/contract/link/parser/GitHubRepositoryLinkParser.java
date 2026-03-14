package backend.academy.linktracker.contract.link.parser;

import backend.academy.linktracker.contract.link.common.ParsedGitHubRepositoryLink;
import backend.academy.linktracker.contract.link.exception.UnsupportedLinkFormatException;
import java.net.URI;

public class GitHubRepositoryLinkParser implements LinkParser<ParsedGitHubRepositoryLink> {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return "github.com".equalsIgnoreCase(host)
            || "www.github.com".equalsIgnoreCase(host);
    }

    @Override
    public ParsedGitHubRepositoryLink parse(URI uri) {
        String path = uri.getPath();
        if (hasNotText(path)) {
            throw new UnsupportedLinkFormatException("Incorrect GitHub link: " + uri);
        }

        String[] segments = path.split("/");
        if (segments.length < 3 || hasNotText(segments[1])
            || hasNotText(segments[2])) {
            throw new UnsupportedLinkFormatException("Incorrect GitHub link: " + uri);
        }

        return new ParsedGitHubRepositoryLink(uri, segments[1], segments[2]);
    }

    private boolean hasNotText(String raw) {
        return raw == null || raw.isBlank();
    }
}
