package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final TrackedLinkRepository trackedLinkRepository;
    private final LinkHandlerRegistry handlerRegistry;

    private final ConcurrentMap<ResourceKey, Object> linkLocks = new ConcurrentHashMap<>();

    public Optional<TrackedLink> findTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);
        return trackedLinkRepository.findByResourceKey(parsedLink.resourceKey());
    }

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        synchronized (lock) {
            return trackedLinkRepository
                    .findByResourceKey(resourceKey)
                    .orElseGet(() -> createTrackedLink(handler, parsedLink));
        }
    }

    public void deleteTrackedLinkWithState(TrackedLink trackedLink) {
        URI uri = URI.create(trackedLink.getUrl());
        LinkHandler handler = handlerRegistry.getHandler(uri);

        ResourceKey resourceKey = trackedLink.getResourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        synchronized (lock) {
            handler.deleteTrackingState(trackedLink);
            trackedLinkRepository.deleteByResourceKey(resourceKey);
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink =
                trackedLinkRepository.save(new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey()));

        try {
            handler.createTrackingState(savedTrackedLink);
            return savedTrackedLink;
        } catch (RuntimeException e) {
            trackedLinkRepository.deleteByResourceKey(parsedLink.resourceKey());
            throw e;
        }
    }
}
