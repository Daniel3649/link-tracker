package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.link.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final TrackedLinkRepository trackedLinkRepository;
    private final LinkHandlerRegistry handlerRegistry;

    private final ConcurrentMap<ResourceKey, Object> linkCreationLocks = new ConcurrentHashMap<>();

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkCreationLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        synchronized (lock) {
            return trackedLinkRepository.findByResourceKey(resourceKey)
                .orElseGet(() -> createTrackedLink(handler, parsedLink));
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink = trackedLinkRepository.save(
            new TrackedLink(
                null,
                parsedLink.url(),
                parsedLink.resourceKey()
            )
        );

        try {
            handler.createTrackingState(savedTrackedLink);
            return savedTrackedLink;
        } catch (RuntimeException e) {
            trackedLinkRepository.deleteByResourceKey(parsedLink.resourceKey());
            throw e;
        }
    }


}
