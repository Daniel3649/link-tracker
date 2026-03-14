package backend.academy.linktracker.contract.link.parser;

import backend.academy.linktracker.contract.link.common.ParsedSupportedLink;
import java.net.URI;

public interface LinkParser<T extends ParsedSupportedLink> {
    boolean supports(URI uri);

    T parse(URI uri);

    static boolean isValidScheme(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"));
    }
}
