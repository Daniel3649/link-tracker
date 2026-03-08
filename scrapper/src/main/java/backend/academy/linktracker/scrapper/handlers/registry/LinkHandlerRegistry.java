package backend.academy.linktracker.scrapper.handlers.registry;

import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkHandlerRegistry {
    private final List<LinkHandler> handlers;

    public LinkHandler getHandler(URI uri) {
        return handlers.stream()
                .filter(handler -> handler.supports(uri))
                .findFirst()
                .orElseThrow(() -> new UnsupportedLinkException("Ссылка не поддерживается: " + uri));
    }
}
