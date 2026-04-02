package backend.academy.linktracker.scrapper.link.parser;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import java.net.URI;

public interface LinkParser {
    boolean supports(URI uri);

    ParsedLink parse(URI uri);

    static boolean isValidScheme(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"));
    }
}
