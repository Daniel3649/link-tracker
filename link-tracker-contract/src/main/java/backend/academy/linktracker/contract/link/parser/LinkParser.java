package backend.academy.linktracker.contract.link.parser;

import backend.academy.linktracker.contract.link.dto.ParsedSupportedLink;

import java.net.URI;

public interface LinkParser<T extends ParsedSupportedLink> {
    boolean supports(URI uri);

    T parse(URI uri);
}
